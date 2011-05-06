package connectionpool

import com.yammer.metrics.{Counter, Timer}
import com.yammer.jmx.JmxManaged

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

class InstrumentedConnectionPool[Conn](name:              String,
                                       connectionFactory: ConnectionFactory[Conn],
                                       max:               Int = 20,
                                       timeout:           Int = 500000)
  extends SimpleConnectionPool[Conn](connectionFactory, max, timeout)
  with JmxManaged {

  val activeConnections = new Counter

  enableJMX("ConnectionPool") { jmx =>
    jmx.addCounter("activeConnections-%s".format(name), activeConnections)
  }

  override def borrow(): Conn = {
    val connection = super.borrow()
    activeConnections.inc
    connection
  }

  override def giveBack(connection: Conn): Unit = {
    super.giveBack(connection)
    activeConnections.dec
  }

  override def invalidate(connection: Conn): Unit = {
    super.invalidate(connection)
    activeConnections.dec
  }
}
