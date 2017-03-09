package com.zendesk.sbtpillar

import com.ecwid.consul.v1.QueryParams
import com.ecwid.consul.v1.health.model.HealthService
import com.ecwid.consul.v1.health.{HealthClient, HealthConsulClient}
import sbt.Logger

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Consul(
  config: ConsulConfig,
  isDevOrTestEnv: Boolean,
  logger: Logger
) {

  /**
    * @param serviceName - e.g. "kafka"
    * @param tag - the tag to filter entries by, generally the pod-id, e.g. "pod100"
    * @param fallBackValues - values to return if no nodes found in consul, e.g. "localhost:8888"
    * @return - returns the node:port mappings found in Consul for the service and tag, otherwise returns the fallBackValues
    */
  def hostsByServiceName(serviceName: String, tag: Option[String], fallBackValues: List[String]): Future[List[String]] = Future {
    logger.info(s"Collecting $serviceName nodes from Consul(${config.host}:${config.port}) for pod/tag=${tag.getOrElse("")}")
    if (isDevOrTestEnv) {
      logger.info(s"returning fallback values $fallBackValues")
      fallBackValues
    } else {
      Try {
        healthServices(serviceName, tag)
      } match {
        case Success(nodes) if nodes.nonEmpty =>
          nodes.map(hs => s"${hs.getNode.getNode}:${hs.getService.getPort}")
        case Success(nodes) if nodes.isEmpty =>
          logger.info(s"No $serviceName nodes were registered with Consul with tag=$tag. Falling back to: $fallBackValues")
          fallBackValues
        case Failure(error) =>
          logger.info(s"Failed to get the $serviceName nodes from Consul. Falling back to: $fallBackValues; error: ${error}")
          fallBackValues
      }
    }
  }

  protected[sbtpillar] def healthServices(serviceName: String, tag: Option[String]): List[HealthService] = {
    val hcc = getHealthConsulClient
    val result = tag match {
      case Some(value) => hcc.getHealthServices(serviceName, value, true, QueryParams.DEFAULT).getValue
      case None => hcc.getHealthServices(serviceName, true, QueryParams.DEFAULT).getValue
    }
    result.toList
  }

  protected[sbtpillar] def getHealthConsulClient: HealthClient = new HealthConsulClient(config.host, config.port)
}
