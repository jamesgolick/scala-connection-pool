package connectionpool.spec

import connectionpool._

import org.specs.Specification
import org.specs.mock.Mockito
import org.mockito.Matchers._

object ObjectFactorySpec extends Specification with Mockito {
  val fakeConn        = FakeConnection("localhost")
  val fakeConnFactory = mock[FakeConnectionFactory]
  val objectFactory   = new ObjectFactory(fakeConnFactory)

  fakeConnFactory.create() returns fakeConn

  "it delegates to the factory for makeObject" in {
    objectFactory.makeObject.asInstanceOf[FakeConnection] must_== fakeConn
  }

  "it validates objects using the factory" in {
    fakeConnFactory.validate(fakeConn) returns true
    objectFactory.validateObject(fakeConn) must beTrue
  }

  "it destroys objects using the factory" in {
    objectFactory.destroyObject(fakeConn)
    there was one(fakeConnFactory).destroy(fakeConn)
  }
}
