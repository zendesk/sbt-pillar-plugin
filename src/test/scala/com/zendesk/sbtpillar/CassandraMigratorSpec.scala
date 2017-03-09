package com.zendesk.sbtpillar

import java.nio.file.{Files, Path}

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import sbt._

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

class CassandraMigratorSpec extends FunSpec with MockitoSugar with Matchers with BeforeAndAfterAll {
  val logger: Logger = mock[Logger]
  val confFile: File = getResourceFile("test.conf")
  val migrationDir: Path = Files.createTempDirectory("pillar")
  val migrator = new CassandraMigrator(confFile, migrationDir.toFile, logger)

  override def beforeAll: Unit = {
    super.beforeAll
    migrator.session.execute("DROP keyspace if exists pillar_test")
  }

  describe("createKeyspace") {
    it("should return success upon creating a new keyspace") {
      keyspaces.contains("pillar_test") should equal(false)
      migrator.createKeyspace
      keyspaces.contains("pillar_test") should equal(true)
    }
  }

  describe("dropKeyspace") {
    it("should return success upon dropping an existing keyspace") {
      migrator.createKeyspace
      keyspaces.contains("pillar_test") should equal(true)
      migrator.dropKeyspace
      keyspaces.contains("pillar_test") should equal(false)
    }
  }

  describe("generateMigration") {
    it("should create a new migration file") {
      migrator.createMigration("foo") should equal(true)
    }
  }


  def keyspaces: List[String] = {
    val keyspaces = try {
      migrator.session.execute(s"SELECT keyspace_name FROM system.schema_keyspaces") // cassandra v2.x
    } catch {
      case NonFatal(_) => migrator.session.execute(s"SELECT keyspace_name FROM system_schema.keyspaces") // cassandra v3+
    }
    keyspaces.all.toList.map(_.getString(0))
  }
}
