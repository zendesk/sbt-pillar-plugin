sbtPlugin := true

name := "sbt-pillar"

lazy val commonSettings = Seq(
  version in ThisBuild := "0.1",
  organization in ThisBuild := "com.42dragons"
)

lazy val root = (project in file(".")).
  settings(
    sbtPlugin := true,
    name := "sbt-pillar",
    description := "A wrapper over the Pillar library to manage Cassandra migrations (https://github.com/comeara/pillar)",
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    publishMavenStyle := false,
    bintrayRepository := "sbt-plugins",
    bintrayOrganization := None
  )

libraryDependencies ++= Seq(
  "com.chrisomeara" %% "pillar" % "2.0.1",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.9"
)
