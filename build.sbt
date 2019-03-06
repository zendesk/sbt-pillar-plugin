sbtPlugin := true

coverageHighlighting := false
scalaVersion := "2.12.8"

name := "sbt-pillar"
organization := "com.zendesk"
description := "A wrapper over the Pillar library to manage Cassandra migrations (https://github.com/comeara/pillar). This fork includes Consul support."
homepage := Some(url("https://github.com/zendesk/sbt-pillar-plugin"))

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
publishMavenStyle := false
publishArtifact in Test := false
pomIncludeRepository := { _ => false}

libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0",
  ("com.ecwid.consul" % "consul-api" % "1.1.11").exclude("commons-logging", "commons-logging"),
  "com.typesafe" % "config" % "1.3.0",
  "de.kaufhof" %% "pillar" % "4.1.2",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalactic" %% "scalactic" % "3.0.6",
  "org.scalatest" %% "scalatest" % "3.0.6" % "test"
)
dependencyOverrides += "org.scalatest" %% "scalatest" % "3.0.6" 

pomExtra :=
  <developers>
    <developer>
      <id>henders</id>
      <name>Shane Hender</name>
      <email>henders [at] gmail.com</email>
      <url>https://henders.github.io</url>
    </developer>
    <developer>
      <id>devstuff</id>
      <name>John Bates</name>
      <email>jbates@zendesk.com</email>
      <url>https://www.zendesk.com</url>
    </developer>
  </developers>

scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/zendesk/sbt-pillar-plugin"),
  connection = "scm:git:git@github.com:zendesk/sbt-pillar-plugin.git"
))

publishTo := {
  val artifactoryDomain = "zdrepo.jfrog.io"
  val artifactoryServerName = "zdrepo"
  val artifactoryRootUrl = s"https://$artifactoryDomain/$artifactoryServerName"
  def repoUrl(id: String) = sbt.url(s"$artifactoryRootUrl/$id")
  val layout = Resolver.ivyStylePatterns
  val prefix = "sbt-plugin"
  // Add timestamp when publishing snapshots to Artifactory; see https://www.jfrog.com/confluence/display/RTF/SBT+Repositories
  val (status, suffix) = if (isSnapshot.value) ("snapshots", ";build.timestamp=" + new java.util.Date().getTime) else ("releases", "")
  val repository = s"$prefix-$status-local"

  Some(Resolver.url(repository, repoUrl(s"$repository$suffix"))(layout))
}
