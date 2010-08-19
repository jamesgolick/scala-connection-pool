package connectionpool

trait LowLevelConnectionPool[Connection] {
  def borrow(): Connection
  def giveBack(conn: Connection): Unit
  def invalidate(conn: Connection): Unit
}
