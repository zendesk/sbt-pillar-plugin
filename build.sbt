sbtPlugin := true

name := "sbt-pillar"
organization := "com.42dragons"
description := "A wrapper over the Pillar library to manage Cassandra migrations (https://github.com/comeara/pillar)"
version := "0.1.2"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayOrganization := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "pillar")

libraryDependencies ++= Seq(
  "com.chrisomeara" %% "pillar" % "2.0.1",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.9"
)
