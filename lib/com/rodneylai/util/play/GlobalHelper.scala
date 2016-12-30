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

import play.api.{Configuration,Environment}
import play.api.libs.mailer._
import play.api.mvc._
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure, Success, Try}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.database._

@Singleton
class GlobalHelper @Inject() (environment:Environment,configuration:Configuration,infoHelper:InfoHelper,exceptionHelper:ExceptionHelper,trackingHelper:TrackingHelper,mailerClient:MailerClient)
{
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  private def getConfigValue(key:String,o:Any):String = key matches "^.*?(password|secret).*?$" match {
    case true => "[ REDACTED ]"
    case false => o.getClass.toString match {
                    case "class com.typesafe.config.impl.ConfigString" => {
                      configuration.getString(key) match {
                        case Some(value) => value
                        case None => "[ UNKNOWN ]"
                      }
                    }
                    case "class com.typesafe.config.impl.ConfigInt" => {
                      configuration.getInt(key) match {
                        case Some(value) => value.toString
                        case None => "[ UNKNOWN ]"
                      }
                    }
                    case "class com.typesafe.config.impl.ConfigDouble" => {
                      configuration.getDouble(key) match {
                        case Some(value) => value.toString
                        case None => "[ UNKNOWN ]"
                      }
                    }
                    case _ => o.toString
    }
  }

  def onStartMsg():Unit = {
    val msg:StringBuilder = new StringBuilder()

    msg.append("StartUp\n\n")
    infoHelper.getBuildDate.map(msg.append("build date: ").append(_).append("\n"))
    msg.append(infoHelper.getMachineInfoString())
    msg.append(infoHelper.getApplicationInfoString())
//    msg.append("plugins: ").append(app.plugins.size).append("\n")
//    app.plugins.foreach { x => msg.append("  ").append(x).append("\n") }
    msg.append("configuration: ").append(configuration.keys.size).append("\n")
    configuration.entrySet.toSeq.sortWith((x, y) => x._1 < y._1).foreach { x => msg.append("  ").append(x._1).append(": ").append(getConfigValue(x._1,x._2)).append("\n") }
    msg.append("\n\n")
    (configuration.getString("email.exceptions"),configuration.getString("email.from.start")) match {
      case (Some(toEmailAddress),Some(fromEmailAddress)) => {
          val email = Email(
                        subject = "Play StartUp",
                        from = "Admin [" + environment.mode.toString + "][" + java.net.InetAddress.getLocalHost.getHostName + "] <" + fromEmailAddress + ">",
                        to = Seq(toEmailAddress),
                        bodyText = Some(msg.toString)
                      )
          Try(mailerClient.send(email)) match {
            case Success(x) => m_log.info(msg.toString)
            case Failure(ex) => exceptionHelper.log(this.getClass,ex,Some("Failed to send startup email"),None,None,false)
          }
      }
      case _ => m_log.info(msg.toString)
    }
  }

  def onHandlerNotFoundMsg(request:RequestHeader,accountOption:Option[Account] = None)(implicit ctx: ExecutionContext):Future[String] = {
    for {
      trackingCountOption <- if ((request.method == "GET") || (request.method == "HEAD") || (request.method == "POST")) {
        trackingHelper.trackEventByTypeAndUrl(
          request,
          request.cookies.get("tracking_id").map(trackingIdCookie => java.util.UUID.fromString(trackingIdCookie.value)).getOrElse(MongoHelper.CONSTANTS.UUID.Empty),
          accountOption.map(_.id).getOrElse(MongoHelper.CONSTANTS.UUID.Empty),
          if (request.method == "POST") "invalid_post" else "page_not_found",
          request.path
        )
      } else {
        Future.successful(None)
      }
    } yield {
      val msg:StringBuilder = new StringBuilder()

      msg.append("General Information [HandlerNotFound][").append(request.path).append("]\n\n")
      trackingCountOption.map(msg.append("repeat count: ").append(_).append("\n"))
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
      msg.append(exceptionHelper.getRequestInfo(request))
      (configuration.getString("email.exceptions"),configuration.getString("email.from.exception")) match {
        case (Some(toEmailAddress),Some(fromEmailAddress)) => {
          val email = Email(
                        subject = "Exception [HandlerNotFound][" + request.method + "][" + request.path + "]",
                        from = "Admin [" + environment.mode.toString + "][" + request.path + "] <" + fromEmailAddress + ">",
                        to = Seq(toEmailAddress),
                        bodyText = Some(msg.toString)
                      )

          Try(mailerClient.send(email)) match {
            case Success (x) => m_log.info(msg.toString)
            case Failure(ex) => exceptionHelper.log(this.getClass,ex,Some("Failed to send handler not found error"),None,Some(request),false)
          }
        }
        case _ => m_log.info(msg.toString)
      }
      trackingCountOption map { trackingCount => if (trackingCount > 5) Thread.sleep(Math.min(trackingCount,30)*1000) }
      "handler_not_found"
    }
  }

  def onBadRequestMsg(request: RequestHeader, error: String, accountOption:Option[Account] = None):Future[String] = {
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
    msg.append(exceptionHelper.getRequestInfo(request))
    (configuration.getString("email.exceptions"),configuration.getString("email.from.exception")) match {
      case (Some(toEmailAddress),Some(fromEmailAddress)) => {
        val email = Email(
                      subject = "Exception [BadRequest][" + error + "]",
                      from = "Admin [" + environment.mode.toString + "][" + request.path + "] <" + fromEmailAddress + ">",
                      to = Seq(toEmailAddress),
                      bodyText = Some(msg.toString)
                    )
        Try(mailerClient.send(email)) match {
          case Success(x) => m_log.info(msg.toString)
          case Failure(ex) => exceptionHelper.log(this.getClass,ex,Some("Failed to send handler bad request error"),None,Some(request),false)
        }
      }
      case _ => m_log.info(msg.toString)
    }
    Future.successful("bad_request")
  }

  def onClientErrorMsg(request: RequestHeader, statusCode: Int, error: String, accountOption:Option[Account] = None):Future[String] = {
    val msg:StringBuilder = new StringBuilder()

    msg.append("General Information [HttpClientError][status_code=").append(statusCode.toString).append("][").append(error).append("]\n\n")
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
    msg.append(exceptionHelper.getRequestInfo(request))
    (configuration.getString("email.exceptions"),configuration.getString("email.from.exception")) match {
      case (Some(toEmailAddress),Some(fromEmailAddress)) => {
        val email = Email(
                      subject = "Exception [HttpClientError][status_code=" + statusCode + "[" + error + "]",
                      from = "Admin [" + environment.mode.toString + "][" + request.path + "] <" + fromEmailAddress + ">",
                      to = Seq(toEmailAddress),
                      bodyText = Some(msg.toString)
                    )
        Try(mailerClient.send(email)) match {
          case Success(x) => m_log.info(msg.toString)
          case Failure(ex) => exceptionHelper.log(this.getClass,ex,Some("Failed to send handler client error"),None,Some(request),false)
        }
      }
      case _ => m_log.info(msg.toString)
    }
    Future.successful("client_error")
  }

  def onErrorMsg(request: RequestHeader, ex: Throwable, accountOption:Option[Account] = None):Future[Unit] = {
    exceptionHelper.log(ex,accountOption,Some(request))
    Future.successful(Unit)
  }
}

class GlobalHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[GlobalHelper]).asEagerSingleton
  }
}
