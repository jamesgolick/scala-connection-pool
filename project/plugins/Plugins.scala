class Plugins(info: sbt.ProjectInfo) extends sbt.PluginDefinition(info) {
  val codaRepo  = "Coda Hale's Repository" at "http://repo.codahale.com/"
  val rsyncSBT  = "com.codahale" % "rsync-sbt" % "0.1.1"
  val jamesRepo = "James Golick's Repository" at "http://repo.jamesgolick.com/"
  val rubySBT   = "com.protose" % "sbt-ruby" % "1.0.5"
}
