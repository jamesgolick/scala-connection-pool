import sbt._
import Process._

class ConnectionPoolProject(info: ProjectInfo) extends DefaultProject(info) {
  val specs    = "org.scala-tools.testing" % "specs_2.8.0" % "1.6.5"
  val mockito  = "org.mockito" % "mockito-all" % "1.8.5"
  val pool     = "commons-pool" % "commons-pool" % "1.5.4" withSources() intransitive()
}
