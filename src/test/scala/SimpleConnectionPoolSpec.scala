package connectionpool.spec

import connectionpool._

import org.specs.Specification
import org.specs.mock.Mockito
import org.mockito.Matchers._

object SimpleConnectionPoolSpec extends Specification with Mockito {
  val fakeConnectionFactory = new FakeConnectionFactory("localhost")
  val max                   = 5
  val simpleConnectionPool  = new SimpleConnectionPool(fakeConnectionFactory, max = max)

  "it supports borrowing of connections" in {
    simpleConnectionPool.borrow() must_== FakeConnection("localhost")
  }

  "it blocks, then raises when there are no resources available" in {
    for(i <- 0 until max)
      simpleConnectionPool.borrow()

    try {
      simpleConnectionPool.borrow()
      false must_== true
    } catch {
      case e: TimeoutError => true must_== true
      case _ => false must_== true
    }
  }

  "it supports invalidating connections" in {
    simpleConnectionPool.invalidate(simpleConnectionPool.borrow())
    for(i <- 0 until max) // if the connection is invalidated, we should still be able to borrow max connections
      simpleConnectionPool.borrow()
    true must_== true
  }

  "it correctly returns connections to the pool" in {
    (0 until 100).foreach { i => simpleConnectionPool() { c => () } }
    true must beTrue // If we get here, we're good
  }
}
