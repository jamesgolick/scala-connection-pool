package connectionpool

class AllNodesDown(message: String) extends Error(message)

object LoadBalancedConnectionPool {
  def apply[Conn](pools:              Seq[LowLevelConnectionPool[Conn]],
                  nodeFailures:       Seq[Class[_ <: Throwable]],
                  maxRetries:         Int = 3,
                  retryDownNodeAfter: Int = 1200,
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

  case class Node(pool: LowLevelConnectionPool[Conn], var downAt: Long = 0) {
    def isUp: Boolean = {
      downAt == 0 || System.currentTimeMillis - downAt > retryDownNodeAfter
    }
  }

  val nodes                    = pools.map { pool => Node(pool) }
  val balancer: Iterator[Node] = Iterator.continually(nodes.iterator).flatten
  val maxAllDownAttempts       = nodes.size * allNodesDownFactor

  def apply[A]()(f: Conn => A): A = apply(1)(f)

  def apply[A](attempt: Int)(f: Conn => A): A = {
    val node       = nextLiveNode()
    val pool       = node.pool
    val connection = pool.borrow()
    
    try {
      val value = f(connection)
      pool.giveBack(connection)
      value
    } catch {
      case e: Throwable if attempt == maxRetries => pool.invalidate(connection); throw e
      case e: Throwable if canRecover(e) => pool.invalidate(connection); failNode(node); apply(attempt + 1)(f)
      case e: Throwable => pool.invalidate(connection); throw e
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
      case node: Node if node.isUp => return node
      case _ => return nextLiveNode(attempt + 1)
    }
  }

  private def failNode(node: Node): Unit = {
    node.downAt = System.currentTimeMillis
  }

  private def allNodesUp: Unit = {
    synchronized {
      nodes.foreach(_.downAt = 0)
    }
  }

  private def nextNode: Node = synchronized { balancer.next }
}
