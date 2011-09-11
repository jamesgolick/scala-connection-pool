package connectionpool

import com.yammer.metrics.Instrumented

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

class InstrumentedConnectionPool[Conn](name:              String,
                                       connectionFactory: ConnectionFactory[Conn],
                                       max:               Int = 20,
                                       timeout:           Int = 500000)
  extends SimpleConnectionPool[Conn](connectionFactory, max, timeout)
  with Instrumented {

  val activeConnections = metrics.counter("activeConnections-%s".format(name))

  override def borrow(): Conn = {
    val connection = super.borrow()
    activeConnections += 1
    connection
  }

  override def giveBack(connection: Conn): Unit = {
    super.giveBack(connection)
    activeConnections -= 1
  }

  override def invalidate(connection: Conn): Unit = {
    super.invalidate(connection)
    activeConnections -= 1
  }
}
