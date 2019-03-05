package com.zendesk.sbtpillar

import java.io.{File => JFile}

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FunSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import sbt.Logger

class ConsulConfigSpec extends FunSpec with MockitoSugar with Matchers {
  val logger: Logger = mock[Logger]
  val configFile: JFile = getResourceFile("test.conf")
  val testConfig: Config = ConfigFactory.parseFile(configFile).resolve().getConfig("test")

  describe("Validation") {
    it("loads the test config") {
      val config = new ConsulConfig(testConfig.getConfig("cassandra.consul"))
      config.host shouldEqual "consul.zd-dev.com"
      config.port shouldEqual 8500
      config.service shouldEqual "foo"
      config.tag shouldEqual Some("taggy")
    }

    it("throws when host is not supplied") {
      intercept[InvalidConsulConfiguration] {
        new ConsulConfig(testConfig.getConfig("test-consul-missing-host"))
      }
    }

    it("throws when port is not supplied") {
      intercept[InvalidConsulConfiguration] {
        new ConsulConfig(testConfig.getConfig("test-consul-missing-port"))
      }
    }

    it("throws when service is not supplied") {
      intercept[InvalidConsulConfiguration] {
        new ConsulConfig(testConfig.getConfig("test-consul-missing-service"))
      }
    }

    it("throws when url.getHost and host disagree") {
      intercept[InvalidConsulConfiguration] {
        new ConsulConfig(testConfig.getConfig("test-consul-url-mismatch-host"))
      }
    }

    it("throws when url.getPort and port disagree") {
      intercept[InvalidConsulConfiguration] {
        new ConsulConfig(testConfig.getConfig("test-consul-url-mismatch-port"))
      }
    }
  }
}
