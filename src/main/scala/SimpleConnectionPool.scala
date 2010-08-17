package connectionpool

import org.apache.commons.pool.impl.GenericObjectPool

class SimpleConnectionPool[Conn](connectionFactory: ConnectionFactory[Conn],
                                 minIdle:   Int = 5,
                                 maxActive: Int = 20,
                                 maxIdle:   Int = 20)
  extends ConnectionPool[Conn] {
  private val objectFactory = new ObjectFactory(connectionFactory)
  private val config        = {
    val c                   = new GenericObjectPool.Config
    c.maxActive             = maxActive
    c.maxIdle               = maxIdle
    c.minIdle               = minIdle
    c.testWhileIdle         = true
    c
  }
  private val pool          = new GenericObjectPool(objectFactory, config)
  

  def apply[A]()(f: Conn => A): A = {
    val connection = pool.borrowObject.asInstanceOf[Conn]

    try {
      f(connection)   
    } finally {
      pool.returnObject(connection)
    }
  }
}
