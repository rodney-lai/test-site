/**
 *
 * Copyright (c) 2015-2016 Rodney S.K. Lai
 * https://github.com/rodney-lai
 *
 * Permission to use, copy, modify, and/or distribute this software for
 * any purpose with or without fee is hereby granted, provided that the
 * above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package com.rodneylai.util

import play.api._
import play.api.mvc._
import play.core._
import scala.collection.{JavaConversions}
import scala.io._
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule

@Singleton
class InfoHelper @Inject() (environment:Environment,configuration:Configuration)
{
  def getBuildDate:Option[String] = {
    if (new java.io.File(environment.rootPath + "/BUILD_DATE").exists) {
      Some(Source.fromFile(environment.rootPath + "/BUILD_DATE").getLines.mkString("\n"))
    } else if (new java.io.File(environment.rootPath + "/../BUILD_DATE").exists) {
      Some(Source.fromFile(environment.rootPath + "/../BUILD_DATE").getLines.mkString("\n"))
    } else {
      None
    }
  }

  def getMachineInfo():Seq[(String,String)] = {
    Seq(
      configuration.getString("docker.home") match {
        case Some(dockerHome) => Some(("docker home",dockerHome))
        case None => None
      },
      configuration.getString("docker.hostname") match {
        case Some(dockerHostName) => Some(("docker hostname",dockerHostName))
        case None => None
      },
      Some(("machine name",java.net.InetAddress.getLocalHost.getHostName)),
      Some(("machine ip",java.net.InetAddress.getLocalHost.getHostAddress))
    ).flatten ++
    JavaConversions.enumerationAsScalaIterator(java.net.NetworkInterface.getNetworkInterfaces()).flatMap(x => JavaConversions.enumerationAsScalaIterator(x.getInetAddresses).map(y => (" ",y.getHostAddress))).toSeq
  }

  def getApplicationInfo():Seq[(String,String)] = {
    Seq(
      ("play version",PlayVersion.current),
      ("play scala version",PlayVersion.scalaVersion),
      ("play sbt version",PlayVersion.sbtVersion),
      ("app scala version",scala.util.Properties.versionNumberString),
      ("app mode",environment.mode.toString),
      ("app path",environment.rootPath.toString)
    )
  }

  def getMachineInfoString():String = {
    getMachineInfo().map({
      case (label,value) if (label.trim.length == 0) => label + value
      case (label,value) if (label.trim.length != 0) => label + ": " + value
    }).mkString("","\n","\n")
  }

  def getApplicationInfoString():String = {
    getApplicationInfo().map({
      case (label,value) if (label.trim.length == 0) => label + value
      case (label,value) if (label.trim.length != 0) => label + ": " + value
    }).mkString("","\n","\n")
  }

}

class InfoHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[InfoHelper]).asEagerSingleton
  }
}
