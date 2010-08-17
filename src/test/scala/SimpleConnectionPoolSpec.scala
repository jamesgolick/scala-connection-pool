package connectionpool.spec

import connectionpool._

import org.specs.Specification
import org.specs.mock.Mockito
import org.mockito.Matchers._

object SimpleConnectionPoolSpec extends Specification with Mockito {
  val fakeConnectionFactory = new FakeConnectionFactory("localhost")
  val simpleConnectionPool  = new SimpleConnectionPool(fakeConnectionFactory)

  "it supports borrowing of connections" in {
    var conn: Option[FakeConnection] = None
    simpleConnectionPool() { c => conn = Some(c) }
    conn must_== Some(FakeConnection("localhost"))
  }

  "it correctly returns connections to the pool" in {
    (0 until 100).foreach { i => simpleConnectionPool() { c => () } }
    true must beTrue // If we get here, we're good
  }
}
