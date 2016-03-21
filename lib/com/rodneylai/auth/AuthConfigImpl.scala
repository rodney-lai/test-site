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

package com.rodneylai.auth

import play.api.mvc._
import play.api.mvc.Results._
import reflect.{ClassTag, classTag}
import scala.concurrent.{ExecutionContext, Future}
import jp.t2v.lab.play2.auth._
import controllers._

trait AuthConfigImpl extends AuthConfig {

  type Id = java.util.UUID

  type User = Account

  type Authority = Role

  val idTag: ClassTag[Id] = classTag[Id]

  val sessionTimeoutInSeconds: Int = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = {
    Account.findById(id)
  }

  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    request.getQueryString("return_url") match {
      case Some(url) => Future.successful(Redirect(url))
      case None => Future.successful(Redirect(routes.Application.index))
    }
  }

  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Redirect(routes.Application.index))
  }

  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Future.successful(Redirect(routes.Application.index))
  }

  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Redirect(routes.Application.index))
  }


  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    (user.role, authority) match {
      case (Role.Administrator, _)            => true
      case (Role.NormalUser, Role.NormalUser) => true
      case _                                  => false
    }
  }

  override lazy val tokenAccessor = new CookieTokenAccessor(
    cookieSecureOption = play.api.Play.current.configuration.getBoolean("auth.cookie.secure").getOrElse(play.api.Play.isProd(play.api.Play.current)),
    cookieMaxAge       = Some(sessionTimeoutInSeconds)
  )

}
