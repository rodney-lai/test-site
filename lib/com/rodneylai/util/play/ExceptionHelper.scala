/**
 *
 * Copyright (c) 2015-2017 Rodney S.K. Lai
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

import play.api.Environment
import play.api.libs.mailer._
import play.api.mvc._
import play.twirl.api.{Html}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._

@Singleton
class ExceptionHelper @Inject() (
  environment:Environment,configuration:ConfigHelper,
  cache:CacheHelper,
  infoHelper:InfoHelper,
  mailerClient:MailerClient
) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def getRequestInfo(request: RequestHeader):String = {
    val msg:StringBuilder = new StringBuilder()

    infoHelper.getBuildDate.map(msg.append("build date: ").append(_).append("\n"))
    msg.append(infoHelper.getMachineInfoString())
    msg.append(infoHelper.getApplicationInfoString())
    msg.append("headers: ").append(request.headers.keys.size).append("\n")
    request.headers.keys.foreach { x => msg.append("  ").append(x).append(": ").append(request.headers.get(x).getOrElse("[ UNKNOWN ]")).append("\n") }
    msg.append("method: ").append(request.method).append("\n")
    msg.append("path: ").append(request.path).append("\n")
    msg.append("queryString: ").append(request.queryString.size).append("\n")
    request.queryString.foreach { x => msg.append("  ").append(x._1).append(": ").append(x._2).append("\n") }
    msg.append("remoteAddress: ").append(request.remoteAddress).append("\n")
    msg.append("version: ").append(request.version).append("\n")
    msg.append("charset: ").append(request.charset.getOrElse("[ UNKNOWN ]")).append("\n")
    msg.append("contentType: ").append(request.contentType.getOrElse("[ UNKNOWN ]")).append("\n")
    msg.append("cookies: ").append(request.cookies.size).append("\n")
    request.cookies.foreach { x => msg.append("  ").append(x.name).append(": ").append(x.value).append("\n") }
    msg.append("domain: ").append(request.domain).append("\n")
    msg.append("mediaType: ").append(request.mediaType.getOrElse("[ UNKNOWN ]")).append("\n")
    msg.append("\n")
    msg.toString
  }

  def log(ex:Throwable,message:Option[String],account: Option[Account],request: Option[RequestHeader],classObjectOption:Option[java.lang.Class[_ <: Any]],mailFlag:Boolean):Unit = {
    val msg:StringBuilder = new StringBuilder()

    if (ex.getCause == null) {
      msg.append("General Information [").append(ex).append("]").append(message.getOrElse("")).append("\n\n")
    } else {
      msg.append("General Information [").append(ex.getCause).append("]").append(message.getOrElse("")).append("\n\n")
    }
    classObjectOption match {
      case Some(classObject) => msg.append("class: ").append(classObject.getName).append("\n")
      case None => { }
    }
    account match {
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
    request match {
      case Some(request) => msg.append(getRequestInfo(request))
      case None => {
        msg.append(infoHelper.getMachineInfoString())
        msg.append(infoHelper.getApplicationInfoString())
      }
    }
    msg.append(ex).append("\n")
    ex.getStackTrace.foreach { x => msg.append(" at ").append(x.toString).append("\n") }
    if (ex.getCause != null) {
      msg.append("\ncaused by ").append(ex.getCause).append("\n")
      ex.getCause.getStackTrace.foreach { x => msg.append(" at ").append(x.toString).append("\n") }
    }
    msg.append("\n\n")
    if (mailFlag) {
      (configuration.getString("email.exceptions"),configuration.getString("email.from.exception")) match {
        case (Some(toEmailAddress),Some(fromEmailAddress)) => {
          val email = Email(
                        subject = Option(ex.getCause) match {
                          case Some(cause) => "Exception [" + cause + "]" + message.getOrElse("")
                          case None => "Exception [" + ex.toString + "]" + message.getOrElse("")
                        },
                        from = request match {
                          case Some(request) => "Admin [" + environment.mode.toString + "][" + request.path + "] <" + fromEmailAddress + ">"
                          case None => "Admin [" + environment.mode.toString + "] <" + fromEmailAddress + ">"
                        },
                        to = Seq(toEmailAddress),
                        bodyText = Some(msg.toString)
                      )

          Try(mailerClient.send(email)) match {
            case Success(x) => {}
            case Failure(ex) => m_log.error("failed to send exception email",ex)
          }
        }
        case _ => {}
      }
    }
    m_log.error(msg.toString)
  }

  def log(ex:Throwable,message:Option[String],account: Option[Account],request: Option[RequestHeader]):Unit = {
    log(ex,message,account,request,None,true)
  }

  def log(classObject:java.lang.Class[_ <: Any],ex:Throwable,message:Option[String],account: Option[Account],request: Option[RequestHeader],mailFlag:Boolean):Unit = {
    log(ex,message,account,request,Some(classObject),mailFlag)
  }

  def log(classObject:java.lang.Class[_ <: Any],ex:Throwable,message:Option[String],account: Option[Account],request: Option[RequestHeader]):Unit = {
    log(ex,message,account,request,Some(classObject),true)
  }

  def log(ex:Throwable,account: Option[Account],request: Option[RequestHeader]):Unit = {
    log(ex,None,account,request,None,true)
  }

  def log(classObject:java.lang.Class[_ <: Any],ex:Throwable,account: Option[Account],request: Option[RequestHeader],mailFlag:Boolean = true):Unit = {
    log(ex,None,account,request,Some(classObject),mailFlag)
  }

  private def getExceptionKey(ex:Throwable):String = {
    if (ex.getCause == null) {
      ex.toString.replaceAll("[^A-Za-z0-9]", "_").toLowerCase
    } else {
      ex.getCause.toString.replaceAll("[^A-Za-z0-9]", "_").toLowerCase
    }
  }

  def log_recurring(classObject:java.lang.Class[_ <: Any],id:String,ex:Throwable,account: Option[Account],request: Option[RequestHeader]):Unit = {
    val recurringExceptionPeriod:Long = cache.get("RecurringExceptionLogPeriodSpy") match {
      case Some(x) => x.asInstanceOf[Long]
      case None => 15
    }
    if (recurringExceptionPeriod > 0) {
      val key:String = "[" + classObject.getName.replaceAll("[^A-Za-z0-9]","_").toLowerCase + "][" + id + "][" + getExceptionKey(ex) + "]"
      val count:Int = cache.get("[recurring_exception_count]" + key) match {
        case Some(x) => {
          val newVal:Int = x.asInstanceOf[Int]+1
          cache.set("[recurring_exception_count]" + key,newVal)
          newVal
        }
        case None => 0
      }

      cache.get("[recurring_exception_time]" + key) match {
        case Some(x) => {
          if (count > 0) {
            log(classObject,ex,Some(" (recurring_count=" + count + ")"),account,request,false)
          } else {
            log(classObject,ex,None,account,request,false)
          }
        }
        case None => {
          cache.set("[recurring_exception_time]" + key,"true",Duration(recurringExceptionPeriod,MINUTES))
          cache.set("[recurring_exception_count]" + key,0)
          if (count > 0) {
            log(classObject,ex,Some(" (recurring_count=" + count + ")"),account,request,true)
          } else {
            log(classObject,ex,None,account,request,true)
          }
        }
      }
    } else {
      log(classObject,ex,account,request)
    }
  }

  def log_warning(classObject:java.lang.Class[_ <: Any],message:String,infoOption:Option[String],account: Option[Account],request: Option[RequestHeader]):Unit = {
    val msg:StringBuilder = new StringBuilder()

    msg.append("General Information [").append(message).append("]\n\n")
    msg.append("class: ").append(classObject.getName).append("\n")
    account match {
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
    request match {
      case Some(request) => msg.append(getRequestInfo(request))
      case None => {
        msg.append(infoHelper.getMachineInfoString())
        msg.append(infoHelper.getApplicationInfoString())
      }
    }
    infoOption map { info => msg.append(message).append("\n").append(info) }
    msg.append("\n\n")
    (configuration.getString("email.exceptions"),configuration.getString("email.from.exception")) match {
      case (Some(toEmailAddress),Some(fromEmailAddress)) => {
        val email = Email(
                      subject = "Warning [" + message + "]",
                      from = request match {
                        case Some(request) => "Admin [" + environment.mode.toString + "][" + request.path + "] <" + fromEmailAddress + ">"
                        case None => "Admin [" + environment.mode.toString + "] <" + fromEmailAddress + ">"
                      },
                      to = Seq(toEmailAddress),
                      bodyText = Some(msg.toString)
                    )

        Try(mailerClient.send(email)) match {
          case Success(x) => {}
          case Failure(ex) => m_log.error("failed to send warning email",ex)
        }
      }
      case _ => {}
    }
    m_log.warn(msg.toString)
  }

}

object ExceptionHelper {

  def getTopMessage(request:RequestHeader):Option[(String,String)] = {
    request.flash.get("error") match {
      case Some(error) if (error == "exception") => Some(("error_icon","Unexpected error."))
      case Some(error) if (error == "handler_not_found") => Some(("error_icon","Page not found."))
      case Some(error) if (error == "bad_request") => Some(("error_icon","Invalid page."))
      case Some(error) if (error == "forgot_password_sent") => Some(("info_icon","Password reset email sent."))
      case Some(error) => Some(("error_icon","Unknown error."))
      case None => None
    }
  }

  def checkForError(request:RequestHeader,html:Html):Result = {
    request.flash.get("error") match {
      case Some(error) if (error == "exception") => Results.InternalServerError(html)
      case Some(error) if (error == "handler_not_found") => Results.NotFound(html)
      case Some(error) if (error == "bad_request") => Results.BadRequest(html)
      case Some(error) if (error == "client_error") => Results.Ok(html)
      case Some(error) if (error == "forgot_password_sent") => Results.Ok(html)
      case None => Results.Ok(html)
    }
  }

}

class ExceptionHelperModule extends AbstractModule {
  override def configure() = {
    bind(classOf[ExceptionHelper]).asEagerSingleton
  }
}
