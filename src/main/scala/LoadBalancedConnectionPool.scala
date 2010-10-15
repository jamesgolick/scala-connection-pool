package connectionpool

import java.util.concurrent.atomic.AtomicLong

class AllNodesDown(message: String) extends Error(message)

object LoadBalancedConnectionPool {
  def apply[Conn](pools:              Seq[LowLevelConnectionPool[Conn]],
                  nodeFailures:       Seq[Class[_ <: Throwable]],
                  maxRetries:         Int = 3,
                  retryDownNodeAfter: Int = 30000,
                  allNodesDownFactor: Int = 2) = {
    new LoadBalancedConnectionPool(pools,
                                   { throwable => nodeFailures.contains(throwable.getClass) },
                                   maxRetries,
                                   retryDownNodeAfter,
                                   allNodesDownFactor)
  }
}

class LoadBalancedConnectionPool[Conn](pools:              Seq[LowLevelConnectionPool[Conn]],
                                       canRecover:         Throwable => Boolean,
                                       maxRetries:         Int,
                                       retryDownNodeAfter: Int,
                                       allNodesDownFactor: Int) 
  extends ConnectionPool[Conn] {

  case class Node(pool: LowLevelConnectionPool[Conn], var downAt: AtomicLong = new AtomicLong(0)) {
    def isUp: Boolean = {
      neverDown || tryAgain
    }

    private def neverDown: Boolean = downAt.get == 0
    private def tryAgain:  Boolean = System.currentTimeMillis - downAt.get > retryDownNodeAfter
  }

  val nodes                    = pools.map { pool => Node(pool) }
  val balancer: Iterator[Node] = Iterator.continually(nodes.iterator).flatten
  val maxAllDownAttempts       = nodes.size * allNodesDownFactor

  def apply[A]()(f: Conn => A): A = apply(1)(f)

  def apply[A](attempt: Int)(f: Conn => A): A = {
    val node       = nextLiveNode()
    val pool       = node.pool
    val connection = pool.borrow()
    var success    = false
    
    try {
      val value = f(connection)
      pool.giveBack(connection)
      success   = true
      value
    } catch {
      case e: Throwable if attempt == maxRetries => throw e
      case e: Throwable if canRecover(e) => failNode(node, node.downAt.get); apply(attempt + 1)(f)
      case e: Throwable => throw e
    } finally {
      if (!success) { pool.invalidate(connection) }
    }
  }

  private def nextLiveNode(attempt: Int = 0): Node = {
    if (nodes.filter(_.isUp).size == 0) {
      attempt match {
        case e: Int if e > maxAllDownAttempts => throw new AllNodesDown("No live nodes.")
        case _                                => allNodesUp
      }
    }

    nextNode match {
      case node: Node if node.isUp =>
        try {
          node.pool.giveBack(node.pool.borrow())
          node
        } catch {
          case e: Throwable if canRecover(e) => 
            failNode(node, node.downAt.get)
            nextLiveNode(attempt + 1)
        }
      case _ => return nextLiveNode(attempt + 1)
    }
  }

  private def failNode(node: Node, compareTo: Long): Unit = {
    node.downAt.compareAndSet(compareTo, System.currentTimeMillis)
  }

  private def allNodesUp: Unit = {
    nodes.foreach(_.downAt.set(0))
  }

  private def nextNode: Node = synchronized { balancer.next }
}
