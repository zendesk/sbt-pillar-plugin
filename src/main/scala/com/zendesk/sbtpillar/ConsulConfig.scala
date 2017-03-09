package com.zendesk.sbtpillar

import java.net.URL

import com.typesafe.config.Config

// Configuration keys:
//
//    host - The host where Consul is running.
//    port - The port Consul is listening on.
//    url - A convenience version of host and port.
//    service - Required; the service name.
//    tag - Optional; for filtering service entries; this is usually the pod, region, etc.
//
// Throws an InvalidConsulConfiguration exception if invalid.
// Requires either the host and port, or the url.
// Throws if the host or port are supplied along with the url, and their values differ.
//
class ConsulConfig(config: Config) {

  private val hostPath = "host"
  private val portPath = "port"
  private val servicePath = "service"
  private val tagPath = "tag"
  private val urlPath = "url"

  val (host: String, port: Int) = {
    val hasHost = config.hasPath(hostPath)
    val hasPort = config.hasPath(portPath)
    val hasUrl = config.hasPath(urlPath)

    if (hasUrl) {
      val url = new URL(config.getString(urlPath))
      if (hasHost) {
        val host = config.getString(hostPath)
        if (url.getHost != host) {
          throw new InvalidConsulConfiguration
        }
      }
      if (hasPort) {
        val port = config.getInt(portPath)
        if (url.getPort != port) {
          throw new InvalidConsulConfiguration
        }
      }
      (url.getHost, url.getPort)
    } else {
      if (!hasHost || !hasPort) {
        throw new InvalidConsulConfiguration
      }
      (config.getString(hostPath), config.getInt(portPath))
    }
  }

  val service: String = {
    if (!config.hasPath(servicePath)) {
      throw new InvalidConsulConfiguration
    }

    config.getString(servicePath)
  }

  val tag: Option[String] = if (config.hasPath(tagPath)) Some(config.getString(tagPath)) else None
}
