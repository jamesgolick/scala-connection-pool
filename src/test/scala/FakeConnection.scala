package connectionpool.spec

import connectionpool._

case class FakeConnection(host: String, var destroyed: Boolean = false) {
  def destroy = destroyed = true
}

class FakeConnectionFactory(host: String) extends ConnectionFactory[FakeConnection] {
  def create(): FakeConnection = FakeConnection(host)
  def validate(conn: FakeConnection): Boolean = true
  def destroy(conn: FakeConnection) = conn.destroy
}
