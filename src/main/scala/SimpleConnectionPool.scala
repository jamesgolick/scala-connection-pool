package connectionpool

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

class TimeoutError(message: String) extends Error(message)
class SimpleConnectionPool[Conn](connectionFactory: ConnectionFactory[Conn],
                                 max:     Int = 20,
                                 timeout: Int = 500000)
  extends ConnectionPool[Conn] with LowLevelConnectionPool[Conn] {

  private val size = new AtomicInteger(0)
  private val pool = new ArrayBlockingQueue[Conn](max)
  
  def apply[A]()(f: Conn => A): A = {
    val connection = borrow

    try {
      f(connection)
    } finally {
      giveBack(connection)
    }
  }

  def borrow(): Conn = {
    pool.poll match {
      case conn: Conn => return conn
      case null       => createOrBlock
    }
  }

  def giveBack(connection: Conn): Unit = {
    pool.offer(connection)
  }

  def invalidate(connection: Conn): Unit = {
    size.decrementAndGet
  }

  private def createOrBlock: Conn = {
    size.get match {
      case e: Int if e == max => block
      case _                  => create
    }
  }

  private def create: Conn = {
    size.incrementAndGet match {
      case e: Int if e > max => size.decrementAndGet; borrow()
      case e: Int            => connectionFactory.create
    }
  }

  private def block: Conn = {
    pool.poll(timeout, TimeUnit.NANOSECONDS) match {
      case conn: Conn => conn
      case _ => throw new TimeoutError("Couldn't acquire a connection in %d nanoseconds.".format(timeout))
    }
  }
}
