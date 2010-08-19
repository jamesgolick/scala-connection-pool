package connectionpool

trait ConnectionPool[Connection] {
  def apply[A]()(f: Connection => A): A
  def borrow(): Connection
  def giveBack(conn: Connection): Unit
  def invalidate(conn: Connection): Unit
}
