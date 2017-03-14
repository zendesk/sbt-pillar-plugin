package com.zendesk.sbtpillar

import java.io.{File => JFile}
import java.nio.file.{Files, Path}

import com.ecwid.consul.v1.health.model.HealthService
import com.ecwid.consul.v1.health.model.HealthService.{Node, Service}
import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar

import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import sbt._

import scala.concurrent.duration._

class ConsulSpec extends FunSpec with MockitoSugar with Matchers with BeforeAndAfterAll with ScalaFutures {
  val logger: Logger = mock[Logger]
  val configFile: JFile = getResourceFile("test.conf")
  val migrationDir: Path = Files.createTempDirectory("pillar")
  val testConfig: Config = ConfigFactory.parseFile(configFile).resolve().getConfig("test")

  describe("#hostsByServiceName") {
    it("always returns fallback values in dev, test, or travis envs") {
      val config = new ConsulConfig(testConfig.getConfig("cassandra.consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = true, logger))
      whenReady(consul.hostsByServiceName("foo", None, List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result shouldEqual List("foo:123", "bar:321")
      }
      verify(consul, never()).getHealthConsulClient
    }

    it("returns fallback values if empty list returned from consul") {
      val config = new ConsulConfig(testConfig.getConfig("cassandra.consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = false, logger))
      doReturn(List()).when(consul).healthServices(any(), any())
      whenReady(consul.hostsByServiceName("foo", None, List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result should equal(List("foo:123", "bar:321"))
      }
      verify(consul, times(1)).healthServices(any(), any())
    }

    it("returns fallback values if failed to connect to consul") {
      val config = new ConsulConfig(testConfig.getConfig("cassandra.consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = false, logger))
      doThrow(new RuntimeException).when(consul).getHealthConsulClient
      whenReady(consul.hostsByServiceName("foo", None, List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result should equal(List("foo:123", "bar:321"))
      }
      verify(consul, times(1)).healthServices(any(), any())
    }

    it("calls out to consul and return fallback values if no entries returned") {
      val config = new ConsulConfig(testConfig.getConfig("cassandra.consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = false, logger))

      val healthService1 = new HealthService
      val node1 = new Node
      node1.setNode("ftp.example.com")
      val service1 = new Service
      service1.setPort(1)
      healthService1.setNode(node1)
      healthService1.setService(service1)

      val healthService2 = new HealthService
      val node2 = new Node
      node2.setNode("www.example.com")
      val service2 = new Service
      service2.setPort(2)
      healthService2.setNode(node2)
      healthService2.setService(service2)

      doReturn(List(healthService1, healthService2)).when(consul).healthServices(any(), any())
      whenReady(consul.hostsByServiceName("foo", Some("pod100"), List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result should equal(List("ftp.example.com:1", "www.example.com:2"))
      }
    }
  }
}
