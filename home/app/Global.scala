/**
 *
 * Copyright (c) 2015-2016 Rodney S.K. Lai
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
import play.api.libs.json.Json
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
    for {
      accountOption <- AuthHelper.getCurrentUser(request)
      _ <- GlobalHelper.onHandlerNotFoundMsg(request,accountOption)
    } yield {
      if ((request.path != null) && (request.path.contains("/services/"))) {
        InternalServerError(Json.toJson("fail"))
      } else {
        Redirect(controllers.routes.Application.index).flashing("error" -> "handler_not_found")
      }
    }
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    for {
      accountOption <- AuthHelper.getCurrentUser(request)
      _ <- GlobalHelper.onBadRequestMsg(request,error,accountOption)
    } yield {
      if ((request.path != null) && (request.path.contains("/services/"))) {
        InternalServerError(Json.toJson("fail"))
      } else {
        Redirect(controllers.routes.Application.index).flashing("error" -> "bad_request")
      }
    }
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    for {
      accountOption <- AuthHelper.getCurrentUser(request)
      _ <- GlobalHelper.onErrorMsg(request,ex,accountOption)
    } yield {
      if ((request.path != null) && (request.path.contains("/services/"))) {
        InternalServerError(Json.toJson("fail"))
      } else {
        Redirect(controllers.routes.Application.index).flashing("error" -> "exception")
      }
    }
  }

}
