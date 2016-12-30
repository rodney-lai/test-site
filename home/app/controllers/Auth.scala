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

package controllers

import play.api._
import play.api.mvc._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try, Random}
import javax.inject.Inject
import jp.t2v.lab.play2.auth._
import com.rodneylai.auth._
import com.rodneylai.database._
import com.rodneylai.stackc._
import com.rodneylai.util._

class Auth @Inject() (implicit override val environment: play.api.Environment,override val configuration:Configuration,override val accountDao:AccountDao,override val trackingHelper:TrackingHelper) extends Controller with TrackingPageView with OptionalAuthElement with AuthConfigImpl with RequireSSL {

  def login = StackAction { implicit request =>
    loggedIn match {
      case Some(account) => Redirect(controllers.routes.Application.index)
      case None => Ok(views.html.login(request,environment))
    }
  }

  def forgot_password = StackAction { implicit request =>
    loggedIn match {
      case Some(account) => Redirect(controllers.routes.Application.index)
      case None => Ok(views.html.forgot_password(request,environment))
    }
  }

  def forgot_password_sent = StackAction { implicit request =>
    Redirect(controllers.routes.Application.index).flashing("error" -> "forgot_password_sent")
  }

  def reset_password(codeString:String) = StackAction { implicit request =>
    loggedIn match {
      case Some(account) => Redirect(controllers.routes.Application.index)
      case None => {
        Try(java.util.UUID.fromString(codeString)) match {
          case Success(code) => {
            Ok(views.html.reset_password(request,environment,code))
          }
          case Failure(ex) => Redirect(controllers.routes.Application.index).flashing("error" -> "handler_not_found")
        }
      }
    }
  }

  def join = StackAction { implicit request =>
    loggedIn match {
      case Some(account) => Redirect(controllers.routes.Application.index)
      case None => Ok(views.html.join(request,environment))
    }
  }

}
