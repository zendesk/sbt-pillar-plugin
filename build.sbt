sbtPlugin := true

coverageHighlighting := false

name := "sbt-pillar"
organization := "com.zendesk"
description := "A wrapper over the Pillar library to manage Cassandra migrations (https://github.com/comeara/pillar). This fork includes Consul support."
homepage := Some(url("https://github.com/zendesk/sbt-pillar-plugin"))

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayOrganization := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "pillar")
publishArtifact in Test := false
pomIncludeRepository := { _ => false}

libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0",
  ("com.ecwid.consul" % "consul-api" % "1.1.11").exclude("commons-logging", "commons-logging"),
  "com.typesafe" % "config" % "1.3.0",
  "de.kaufhof" %% "pillar" % "3.1.0",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

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
