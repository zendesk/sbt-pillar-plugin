package com.zendesk.sbtpillar

import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

import com.typesafe.config.Config
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import sbt._

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

// scalastyle:ignore magic.number

class TestCassandraMigrator(configFile: File, migrationsDir: File, logger: Logger, configSection: String)
  extends CassandraMigrator(configFile, migrationsDir, logger) {

  override protected def getCassandraConfig: Config = {
    config.getConfig(configSection)
  }
}

class CassandraMigratorSpec extends FunSpec with MockitoSugar with Matchers with BeforeAndAfterAll {
  val logger: Logger = mock[Logger]
  val confFile: File = getResourceFile("test.conf")
  val migrationDir: Path = Files.createTempDirectory("pillar")
  val migrator = new CassandraMigrator(confFile, migrationDir.toFile, logger)

  override def beforeAll: Unit = {
    super.beforeAll
    migrator.session.execute("DROP keyspace if exists pillar_test")
  }

  describe("hostsAndPorts") {
    it("should handle multiple hosts (name only)") {
      val subject = new TestCassandraMigrator(confFile, migrationDir.toFile, logger, "test-cassandra-multiple-hosts")
      val expectedResult = List(
        new InetSocketAddress("cassandra1", 9042),
        new InetSocketAddress("cassandra2", 9042)
      )
      subject.hostsAndPorts shouldEqual expectedResult
    }
    it("should handle multiple hosts (explicit ports)") {
      val subject = new TestCassandraMigrator(confFile, migrationDir.toFile, logger, "test-cassandra-explicit-ports")
      val expectedResult = List(
        new InetSocketAddress("cassandra1", 9091),
        new InetSocketAddress("cassandra2", 9092),
        new InetSocketAddress("cassandra3", 9093)
      )
      subject.hostsAndPorts shouldEqual expectedResult
    }
    it("should handle multiple hosts (with and without ports)") {
      val subject = new TestCassandraMigrator(confFile, migrationDir.toFile, logger, "test-cassandra-mixed-ports")
      val expectedResult = List(
        new InetSocketAddress("cassandra1", 9042),
        new InetSocketAddress("cassandra3", 9099)
      )
      subject.hostsAndPorts shouldEqual expectedResult
    }
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
