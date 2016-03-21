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

package com.rodneylai.stackc

import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}
import scala.util.{Failure, Success, Try}
import jp.t2v.lab.play2.stackc._

trait TrackingCookie extends StackableController {
  self: Controller =>

  type TrackingId = java.util.UUID

  case object TrackingIdKey extends RequestAttributeKey[TrackingId]

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    req.cookies.get("tracking_id") match {
      case Some(trackingIdCookie) => {
        Try(java.util.UUID.fromString(trackingIdCookie.value)) match {
          case Success(trackingId) => super.proceed(req.set(TrackingIdKey,trackingId))(f)
          case Failure(ex) => {
            val trackingId:java.util.UUID = java.util.UUID.randomUUID

            super.proceed(req.set(TrackingIdKey,trackingId))(f).map(result => result.withCookies(Cookie(name = "tracking_id", value = trackingId.toString, maxAge = Some(Int.MaxValue))))
          }
        }
      }
      case None => {
        val trackingId:java.util.UUID = java.util.UUID.randomUUID

        super.proceed(req.set(TrackingIdKey,trackingId))(f).map(result => result.withCookies(Cookie(name = "tracking_id", value = trackingId.toString, maxAge = Some(Int.MaxValue))))
      }
    }
  }

  implicit def trackingId(implicit req: RequestWithAttributes[_]): TrackingId = req.get(TrackingIdKey).get
}
