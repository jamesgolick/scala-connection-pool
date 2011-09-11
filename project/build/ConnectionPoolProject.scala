import sbt._
import Process._

class ConnectionPoolProject(info: ProjectInfo) extends DefaultProject(info) with rsync.RsyncPublishing with ruby.GemBuilding {
  val codaRepo  = "Coda Hale's Repository" at "http://repo.codahale.com/"
  val specs     = "org.scala-tools.testing" % "specs_2.8.0" % "1.6.5"
  val mockito   = "org.mockito" % "mockito-all" % "1.8.5"
  val metrics   = "com.yammer.metrics" %% "metrics-scala" % "2.0.0-BETA16" withSources()

  /**
   * Gem build settings.
   */
  val gemAuthor             = "James Golick"
  val gemAuthorEmail        = "jamesgolick@gmail.com"
  override val gemVersion   = "0.0.2"
  override lazy val gemName = "load_balancing_connection_pool"

  /**
   * Include docs and source as build artifacts.
   */
  override def packageSrcJar = defaultJarPath("-sources.jar")
  val sourceArtifact = sbt.Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageSrc, `package`)

  def rsyncRepo = "james@jamesgolick.com:/var/www/repo.jamesgolick.com"
}
