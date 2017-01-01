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

package com.rodneylai.security

import play.api.mvc.{Request, Result, Results}
import play.api.mvc.Results._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DynamicResourceHandler, DeadboltHandler}
import be.objectify.deadbolt.scala.models.Subject
import com.rodneylai.auth.Account
import com.rodneylai.security.models.User

class DefaultDeadboltHandler(accountOption:Option[Account] = None,dynamicResourceHandler: Option[DynamicResourceHandler] = None) extends DeadboltHandler {

  def beforeAuthCheck[A](request: Request[A]) = Future(None)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = Future(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = {
    accountOption match {
      case Some(account) => Future(Some(new User(account.email,account.roleList)))
      case None => Future(None)
    }
  }

  def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    if ((request.path != null) && (request.path.contains("/services/"))) {
      Future(InternalServerError("kick"))
    } else {
      Future.successful(Results.Redirect(controllers.routes.Application.index))
    }
  }
}
