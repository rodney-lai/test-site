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

import play.api.{Application,GlobalSettings}
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}
import java.util.{TimeZone}
import com.rodneylai.auth._
import com.rodneylai.util._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    GlobalHelper.onStartMsg(app)
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    AuthHelper.initTestUsers
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    AuthHelper.getCurrentUser(request) map {
      accountOption => GlobalHelper.onHandlerNotFoundMsg(request,accountOption)
    } recover {
      case ex:Exception => {
        GlobalHelper.onHandlerNotFoundMsg(request)
      }
    }
    if (request.path.contains("/services/")) {
      Future(InternalServerError("fail"))
    } else {
      Future.successful(Redirect(controllers.routes.Application.index).flashing("error" -> "handler_not_found"))
    }
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    AuthHelper.getCurrentUser(request) map {
      accountOption => GlobalHelper.onBadRequestMsg(request,error,accountOption)
    } recover {
      case ex:Exception => {
        GlobalHelper.onBadRequestMsg(request,error)
      }
    }
    if (request.path.contains("/services/")) {
      Future(InternalServerError("fail"))
    } else {
      Future.successful(Redirect(controllers.routes.Application.index).flashing("error" -> "bad_request"))
    }
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    AuthHelper.getCurrentUser(request) map {
      accountOption => GlobalHelper.onErrorMsg(request,ex,accountOption)
    } recover {
      case innerException:Exception => {
        GlobalHelper.onErrorMsg(request,ex)
      }
    }
    if (request.path.contains("/services/")) {
      Future(InternalServerError("fail"))
    } else {
      Future.successful(Redirect(controllers.routes.Application.index).flashing("error" -> "exception"))
    }
  }

}

