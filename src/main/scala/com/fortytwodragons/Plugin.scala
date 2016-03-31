package com.fortytwodragons

import sbt.Keys._
import sbt._

object Plugin extends AutoPlugin {

  object autoImport {
    //  available tasks
    val createKeyspace = taskKey[Unit]("Create keyspace.")
    val dropKeyspace = taskKey[Unit]("Drop keyspace.")
    val migrate = taskKey[Unit]("Run pillar migrations.")
    val cleanMigrate = taskKey[Unit]("Recreate keyspace and run pillar migrations.")
    //  build settings declared in the user's build.sbt
    val pillarConfigFile = settingKey[File]("Path to the configuration file with the cassandra settings")
    val pillarMigrationsDir = settingKey[File]("Path to the directory with the migration files")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val buildSettings = Seq(
    pillarConfigFile := file("db/pillar.conf"),
    pillarMigrationsDir := file("db/migrations"),
    createKeyspace := createKeyspaceTask.value,
    dropKeyspace := dropKeyspaceTask.value,
    migrate := migrateTask.value,
    cleanMigrate := cleanMigrateTask.value)

  lazy val createKeyspaceTask = Def.task {
    new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log).createKeyspace
  }

  lazy val dropKeyspaceTask = Def.task {
    new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log).dropKeyspace
  }

  lazy val migrateTask = Def.task {
    new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log).migrate
  }

  lazy val cleanMigrateTask = Def.task {
    val task = new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log)
    task
      .dropKeyspace
      .createKeyspace
      .migrate
  }
}
