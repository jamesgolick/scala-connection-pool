package connectionpool

trait ConnectionPool[Connection] {
  def apply[A]()(f: Connection => A): A
}
