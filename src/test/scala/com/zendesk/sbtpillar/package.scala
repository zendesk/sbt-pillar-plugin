package com.zendesk

import java.io.{File => JFile}

package object sbtpillar {

  // SBT has differences with the class loaders vs running jre directly, gah!
  def getResourceFile(fileName: String): JFile = {
    val url = Option(getClass.getClassLoader.getResource(fileName)) match {
      case Some(x) => x
      case _ => ClassLoader.getSystemClassLoader.getResource(fileName)
    }
    new JFile(url.toURI)
  }
}
