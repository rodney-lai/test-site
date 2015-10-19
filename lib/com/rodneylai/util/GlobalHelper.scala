/**
 *
 * Copyright (c) 2015 Rodney S.K. Lai
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
import play.api.libs.mailer._
import play.api.mvc._
import play.api.Play.current
import scala.util.{Failure, Success, Try}
import com.rodneylai.auth._

object GlobalHelper
{
  private val m_mailer:MailerClient = new CommonsMailer(play.api.Play.current.configuration)

  private def getConfigValue(app:Application,key:String,o:Any):String = key matches "^.*?(password|secret).*?$" match {
    case true => "[ REDACTED ]"
    case false => o.getClass.toString match {
                    case "class com.typesafe.config.impl.ConfigString" => {
                      app.configuration.getString(key) match {
                        case Some(value) => value
                        case None => "[ UNKNOWN ]"
                      }
                    }
                    case "class com.typesafe.config.impl.ConfigInt" => {
                      app.configuration.getInt(key) match {
                        case Some(value) => value.toString
                        case None => "[ UNKNOWN ]"
                      }
                    }
                    case "class com.typesafe.config.impl.ConfigDouble" => {
                      app.configuration.getDouble(key) match {
                        case Some(value) => value.toString
                        case None => "[ UNKNOWN ]"
                      }
                    }
                    case _ => o.toString
    }
  }

  def onStartMsg(app: Application):Unit = {
    val msg:StringBuilder = new StringBuilder()

    println("onStart")
    msg.append("StartUp\n\n")
    msg.append(InfoHelper.getMachineInfoString)
    msg.append(InfoHelper.getApplicationInfoString)
    msg.append("plugins: ").append(app.plugins.size).append("\n")
    app.plugins.foreach { x => msg.append("  ").append(x).append("\n") }
    msg.append("configuration: ").append(app.configuration.keys.size).append("\n")
    app.configuration.entrySet.toSeq.sortWith((x, y) => x._1 < y._1).foreach { x => msg.append("  ").append(x._1).append(": ").append(getConfigValue(app,x._1,x._2)).append("\n") }
    msg.append("\n\n")
    (app.configuration.getString("email.exceptions"),app.configuration.getString("email.from.start")) match {
      case (Some(toEmailAddress),Some(fromEmailAddress)) => {
          val email = Email(
                        subject = "Play StartUp",
                        from = "Admin [" + play.api.Play.current.mode.toString + "][" + java.net.InetAddress.getLocalHost.getHostName + "] <" + fromEmailAddress + ">",
                        to = Seq(toEmailAddress),
                        bodyText = Some(msg.toString)
                      )
          Try(m_mailer.send(email)) match {
            case Success(x) => Logger.info(msg.toString)
            case Failure(ex) => ExceptionHelper.log(this.getClass,ex,Some("Failed to send startup email"),None,None,false)
          }
      }
      case _ => Logger.info(msg.toString)
    }
  }

  def onHandlerNotFoundMsg(request: RequestHeader,accountOption:Option[Account] = None):Unit = {
    val msg:StringBuilder = new StringBuilder()

    msg.append("General Information [HandlerNotFound][").append(request.path).append("]\n\n")
    accountOption match {
      case Some(account) => {
        msg.append("user:\n")
        msg.append("  id: ").append(account.id).append("\n")
        msg.append("  email: ").append(account.email).append("\n")
        msg.append("  name: ").append(account.name).append("\n")
      }
      case None => {
        msg.append("user: [ UNKNOWN ]\n")
      }
    }
    msg.append(ExceptionHelper.getRequestInfo(request))
    (play.api.Play.current.configuration.getString("email.exceptions"),play.api.Play.current.configuration.getString("email.from.exception")) match {
      case (Some(toEmailAddress),Some(fromEmailAddress)) => {
        val email = Email(
                      subject = "Exception [HandlerNotFound][" + request.path + "]",
                      from = "Admin [" + play.api.Play.current.mode.toString + "][" + request.path + "] <" + fromEmailAddress + ">",
                      to = Seq(toEmailAddress),
                      bodyText = Some(msg.toString)
                    )

        Try(m_mailer.send(email)) match {
          case Success(x) => Logger.info(msg.toString)
          case Failure(ex) => ExceptionHelper.log(this.getClass,ex,Some("Failed to send handler not found error"),None,Some(request),false)
        }
      }
      case _ => Logger.info(msg.toString)
    }
  }

  def onBadRequestMsg(request: RequestHeader, error: String, accountOption:Option[Account] = None):Unit = {
    val msg:StringBuilder = new StringBuilder()

    msg.append("General Information [BadRequest][").append(error).append("]\n\n")
    accountOption match {
      case Some(account) => {
        msg.append("user:\n")
        msg.append("  id: ").append(account.id).append("\n")
        msg.append("  email: ").append(account.email).append("\n")
        msg.append("  name: ").append(account.name).append("\n")
      }
      case None => {
        msg.append("user: [ UNKNOWN ]\n")
      }
    }
    msg.append(ExceptionHelper.getRequestInfo(request))
    (play.api.Play.current.configuration.getString("email.exceptions"),play.api.Play.current.configuration.getString("email.from.exception")) match {
      case (Some(toEmailAddress),Some(fromEmailAddress)) => {
        val email = Email(
                      subject = "Exception [BadRequest][" + error + "]",
                      from = "Admin [" + play.api.Play.current.mode.toString + "][" + request.path + "] <" + fromEmailAddress + ">",
                      to = Seq(toEmailAddress),
                      bodyText = Some(msg.toString)
                    )
        Try(m_mailer.send(email)) match {
          case Success(x) => Logger.info(msg.toString)
          case Failure(ex) => ExceptionHelper.log(this.getClass,ex,Some("Failed to send handler bad request error"),None,Some(request),false)
        }
      }
      case _ => Logger.info(msg.toString)
    }
  }

  def onErrorMsg(request: RequestHeader, ex: Throwable, accountOption:Option[Account] = None):Unit = {
    ExceptionHelper.log(ex,accountOption,Some(request))
  }
}


