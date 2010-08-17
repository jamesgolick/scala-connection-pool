package connectionpool

import org.apache.commons.pool.BasePoolableObjectFactory

class ObjectFactory[Conn](connectionFactory: ConnectionFactory[Conn])
  extends BasePoolableObjectFactory {
  override def makeObject: Object = {
    connectionFactory.create().asInstanceOf[Object]
  }

  override def validateObject(client: Any): Boolean = {
    connectionFactory.validate(client.asInstanceOf[Conn])
  }

  override def destroyObject(client: Any): Unit = {
    connectionFactory.destroy(client.asInstanceOf[Conn])
  }
}
