package connectionpool.spec

import connectionpool._

import org.specs.Specification
import org.specs.mock.Mockito
import org.mockito.Matchers._

object LoadBalancedConnectionPoolSpec extends Specification with Mockito {
  class RecoverableError extends Error("Recoverable, bro")
  class TimeoutError extends Error("Couldn't connect, bro")

  class FakeConnectionPool extends LowLevelConnectionPool[FakeConnection] {
    var connection: FakeConnection = _
    var borrows                    = 0

    def apply[A]()(f: FakeConnection => A): A = {
      f(connection)
    }

    def borrow(): FakeConnection = { borrows += 1; connection }
    def invalidate(connection: FakeConnection): Unit = { }
    def giveBack(conn: FakeConnection): Unit = { }
  }

  class TimingOutConnectionPool extends FakeConnectionPool {
    override def borrow(): FakeConnection = {
      borrows += 1
      throw new TimeoutError()
    }
  }

  val poolOne          = new FakeConnectionPool
  val poolTwo          = new FakeConnectionPool
  val loadBalancedPool = LoadBalancedConnectionPool(List(poolOne, poolTwo),
                                                    List(classOf[RecoverableError]))

  "borrowing a connection" in {
    val poolOneConnection = mock[FakeConnection]
    val poolTwoConnection = mock[FakeConnection]
    poolOne.connection    = poolOneConnection
    poolTwo.connection    = poolTwoConnection

    "rotates between the pools" in {
      loadBalancedPool() { connection => connection must_== poolOneConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolOneConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
    }

    "retries with the next connection on failure" in {
      var attempt = 0
      loadBalancedPool() { connection =>
        attempt match {
          case 0 => attempt += 1; throw new RecoverableError()
          case _ => "asdf"
        }
      } must_== "asdf"
    }

    "attempts until maxRetries is reached" in {
      var attempt: Int = 0

      try {
        loadBalancedPool() { connection =>
          attempt += 1
          throw new RecoverableError
       }
        false must beTrue
      } catch {
        case e: RecoverableError => true must beTrue
        case _ => false must beTrue
      }
      attempt must_== 3
    }

    "removes a node from the pool after a recoverable exception" in {
      var attempt = 0
      loadBalancedPool() { connection =>
        attempt match {
          case 0 => attempt += 1; throw new RecoverableError()
          case _ => "asdf"
        }
      }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
    }

    "when all the nodes are down, it marks them all as up again" in {
      var attempt = 0
      loadBalancedPool() { connection =>
        attempt match {
          case 0 => attempt += 1; throw new RecoverableError()
          case 1 => attempt += 1; throw new RecoverableError()
          case _ => "asdf"
        }
      }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolOneConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
    }

    "with a canRecover function, removes a node from the pool after a recoverable exception" in {
      val loadBalancedPool = new LoadBalancedConnectionPool(List(poolOne, poolTwo),
                                                           { throwable => true },
                                                           maxRetries         = 3,
                                                           retryDownNodeAfter = 1200,
                                                           allNodesDownFactor = 2)
      var attempt = 0
      loadBalancedPool() { connection =>
        attempt match {
          case 0 => attempt += 1; throw new RecoverableError()
          case _ => "asdf"
        }
      }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
    }
  }

  "when a connection times out on borrow" in {
    val poolOne          = new TimingOutConnectionPool
    val poolTwo          = new FakeConnectionPool
    val loadBalancedPool = LoadBalancedConnectionPool(List(poolOne, poolTwo),
                                                      List(classOf[RecoverableError], classOf[TimeoutError]))

    val poolOneConnection = mock[FakeConnection]
    val poolTwoConnection = mock[FakeConnection]
    poolOne.connection    = poolOneConnection
    poolTwo.connection    = poolTwoConnection

    "it recovers from the error and fails the node" in {
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      loadBalancedPool() { connection => connection must_== poolTwoConnection }
      poolOne.borrows must_== 1
    }
  }

  "when a connection times out on borrow and the error is not recoverable" in {
    val poolOne          = new TimingOutConnectionPool
    val poolTwo          = new FakeConnectionPool
    val loadBalancedPool = LoadBalancedConnectionPool(List(poolOne, poolTwo),
                                                      List(classOf[RecoverableError]))

    val poolOneConnection = mock[FakeConnection]
    val poolTwoConnection = mock[FakeConnection]
    poolOne.connection    = poolOneConnection
    poolTwo.connection    = poolTwoConnection

    "it reraises the exception" in {
      try {
        loadBalancedPool() { connection => }
      } catch {
        case e: TimeoutError => true must beTrue
        case _               => false must beTrue
      }
    }
  }
}
