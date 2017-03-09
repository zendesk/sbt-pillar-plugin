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
      val config = new ConsulConfig(testConfig.getConfig("consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = true, logger))
      whenReady(consul.hostsByServiceName("foo", None, List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result shouldEqual List("foo:123", "bar:321")
      }
      verify(consul, never()).getHealthConsulClient
    }

    it("returns fallback values if empty list returned from consul") {
      val config = new ConsulConfig(testConfig.getConfig("consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = false, logger))
      doReturn(List()).when(consul).healthServices(any(), any())
      whenReady(consul.hostsByServiceName("foo", None, List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result should equal(List("foo:123", "bar:321"))
      }
      verify(consul, times(1)).healthServices(any(), any())
    }

    it("returns fallback values if failed to connect to consul") {
      val config = new ConsulConfig(testConfig.getConfig("consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = false, logger))
      doThrow(new RuntimeException).when(consul).getHealthConsulClient
      whenReady(consul.hostsByServiceName("foo", None, List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result should equal(List("foo:123", "bar:321"))
      }
      verify(consul, times(1)).healthServices(any(), any())
    }

    it("calls out to consul and return fallback values if no entries returned") {
      val config = new ConsulConfig(testConfig.getConfig("consul"))
      val consul = spy(new Consul(config, isDevOrTestEnv = false, logger))
      val healthService = new HealthService
      val node = new Node
      node.setNode("example.com")
      val service = new Service
      service.setPort(1)
      healthService.setNode(node)
      healthService.setService(service)
      doReturn(List(healthService)).when(consul).healthServices(any(), any())
      whenReady(consul.hostsByServiceName("foo", Some("pod100"), List("foo:123", "bar:321")), Timeout(1.second)) { result =>
        result should equal(List("example.com:1"))
      }
    }
  }
}
