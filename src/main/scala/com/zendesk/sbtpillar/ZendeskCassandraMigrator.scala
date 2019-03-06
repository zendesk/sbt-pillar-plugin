package com.zendesk.sbtpillar

import java.io.PrintWriter
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.datastax.driver.core.exceptions.InvalidQueryException
import com.datastax.driver.core.{Cluster, ConsistencyLevel, QueryOptions, Session}
import com.typesafe.config.{Config, ConfigFactory}
import de.kaufhof.pillar._
import sbt.{Logger, _}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class ZendeskCassandraMigrator(configFile: File, migrationsDir: File, logger: Logger) {
  val env: String = sys.props.getOrElse("SCALA_ENV", sys.env.getOrElse("SCALA_ENV", "test"))
  def isDevOrTestEnv: Boolean = List("development", "test", "travis").contains(env)
  logger.info(s"Loading config file: $configFile for environment: $env")
  val config: Config = ConfigFactory.parseFile(configFile).resolve().getConfig(env)
  val cassandraConfig: Config = getCassandraConfig
  val hostsAndPorts: Seq[InetSocketAddress] = {
    val port: Int = cassandraConfig.getInt("port")
    val portRegex = "(.*):([0-9]{1,5})".r.anchored
    val fallbackValues = cassandraConfig.getString("hosts").split(',').map(h => h.trim).toList
    // This assumes each fallback value is either a DNS host name or an IPv4 address, with an optional colon and port suffix.
    // IPv6 addresses will break this code.

    val retrievedHosts = if (cassandraConfig.hasPath("consul")) {
      val consulConfig = new ConsulConfig(cassandraConfig.getConfig("consul"))
      val consul = new Consul(consulConfig, isDevOrTestEnv, logger)
      val consulResolvedHosts = consul.hostsByServiceName(
        consulConfig.service,
        consulConfig.tag,
        fallbackValues
      )
      Await.result(consulResolvedHosts, 10.seconds)
    } else {
      fallbackValues
    }

    retrievedHosts.map {
      case portRegex(h, p) => new InetSocketAddress(h, p.toInt)
      case f => new InetSocketAddress(f, port)
    }
  }
  val keyspace: String = cassandraConfig.getString("keyspace")
  val replicationStrategy: String = Try(cassandraConfig.getString("replicationStrategy"))
    .getOrElse(ZendeskCassandraMigrator.DefaultReplicationStrategy)
  val replicationFactor: Int = Try(cassandraConfig.getInt("replicationFactor"))
    .getOrElse(ZendeskCassandraMigrator.DefaultReplicationFactor)
  val defaultConsistencyLevel: ConsistencyLevel = Try(ConsistencyLevel.valueOf(cassandraConfig.getString("defaultConsistencyLevel")))
    .getOrElse(ZendeskCassandraMigrator.DefaultConsistencyLevel)
  val username: String = Try(cassandraConfig.getString("username")).getOrElse(ZendeskCassandraMigrator.DefaultUsername)
  val password: String = Try(cassandraConfig.getString("password")).getOrElse(ZendeskCassandraMigrator.DefaultPassword)
  lazy val session: Session = createSession

  def createKeyspace: ZendeskCassandraMigrator = {
    logger.info(s"Creating keyspace $keyspace at ${hostsAndPorts.head}")
    val migrator = new CassandraMigrator(Registry(Seq.empty), "applied_migrations")

    migrator.initialize(session, keyspace, SimpleStrategy(replicationFactor))
    this
  }

  def dropKeyspace: ZendeskCassandraMigrator = {
    logger.info(s"Dropping keyspace $keyspace at ${hostsAndPorts.head}")
    try {
      val migrator = new CassandraMigrator(Registry(Seq.empty), "applied_migrations")
      migrator.destroy(session, keyspace)
    } catch {
      case e: InvalidQueryException => logger.warn(s"Failed to drop keyspace ($keyspace) - ${e.getMessage}")
    }
    this
  }

  def migrate: ZendeskCassandraMigrator = {
    val registry = Registry.fromDirectory(migrationsDir)
    logger.info(s"Migrating keyspace $keyspace at ${hostsAndPorts.head}")
    session.execute(s"USE $keyspace")
    val migrator = new CassandraMigrator(registry, "applied_migrations")

    migrator.migrate(session)
    this
  }

  def createMigration(name: String): Boolean = {
    val now = LocalDateTime.now
    val creationDate: String = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    val migrationFileName: String = s"$migrationsDir/${creationDate}_$name.cql"
    logger.info(s"Creating migration file: $migrationFileName")
    val migrationTemplate: String = s"""-- description: $name
-- authoredAt: ${System.currentTimeMillis}
-- up:

###UP MIGRATION###

-- down:

###DOWN MIGRATION###

"""
    new PrintWriter(migrationFileName) {
      write(migrationTemplate)
      close()
    }
    logger.success(s"Created migration '$migrationFileName'")
    true
  }

  private def createSession = {
    val queryOptions = new QueryOptions()
    queryOptions.setConsistencyLevel(defaultConsistencyLevel)

    Cluster
      .builder()
      .addContactPointsWithPorts(hostsAndPorts.asJavaCollection)
      .withCredentials(username, password)
      .withQueryOptions(queryOptions)
      .build()
      .connect
  }

  protected def getCassandraConfig: Config = {
    config.getConfig("cassandra")
  }
}

object ZendeskCassandraMigrator {
  val DefaultConsistencyLevel = ConsistencyLevel.QUORUM
  val DefaultReplicationStrategy = "SimpleStrategy"
  val DefaultReplicationFactor = 3
  val DefaultUsername = "cassandra"
  val DefaultPassword = "cassandra"
}
