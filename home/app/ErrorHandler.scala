
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

import play.api.http.{HttpErrorHandler,Status}
import play.api.libs.json.Json
import play.api.libs.mailer._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.util._

class ErrorHandler @Inject() (globalHelper:GlobalHelper,authHelper:AuthHelper) extends HttpErrorHandler {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def onClientError(request: RequestHeader, statusCode: Int, errorMessage: String) = {
    for {
      accountOption <- authHelper.getCurrentUser(request)
      errorCode <- statusCode match {
        case Status.BAD_REQUEST => globalHelper.onBadRequestMsg(request,errorMessage,accountOption)
        case Status.NOT_FOUND => globalHelper.onHandlerNotFoundMsg(request,accountOption)
        case _ => globalHelper.onClientErrorMsg(request,statusCode,errorMessage,accountOption)
      }
    } yield {
      if ((request.path != null) && (request.path.contains("/services/"))) {
        InternalServerError(Json.toJson("fail"))
      } else {
        Redirect(controllers.routes.Application.index).flashing("error" -> errorCode)
      }
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    for {
      accountOption <- authHelper.getCurrentUser(request)
      _ <- globalHelper.onErrorMsg(request,exception,accountOption)
    } yield {
      if ((request.path != null) && (request.path.contains("/services/"))) {
        InternalServerError(Json.toJson("fail"))
      } else {
        Redirect(controllers.routes.Application.index).flashing("error" -> "exception")
      }
    }
  }
}
