package com.zendesk.sbtpillar

// Thrown if the Consul configuration is invalid:
//  - url supplied along with host and/or port, but they have differing values.
//  - host without port, or vice versa.
class InvalidConsulConfiguration(reason: String) extends RuntimeException(s"Invalid Consul configuration: $reason")
