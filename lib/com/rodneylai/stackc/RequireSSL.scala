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
import jp.t2v.lab.play2.auth._
import jp.t2v.lab.play2.stackc._
import com.rodneylai.auth._

trait RequireSSL extends StackableController {
  self: Controller =>

  override def proceed[A](req: RequestWithAttributes[A])(f: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    if (req.secure) {
      super.proceed(req)(f)
    } else if (play.api.Play.isDev(play.api.Play.current)) {
      play.api.Play.current.configuration.getInt("https.port") match {
        case Some(port) => Future(Redirect("https://" + req.domain + ":" + port.toString + req.uri))
        case None => super.proceed(req)(f)
      }
    } else if (play.api.Play.current.configuration.getBoolean("auth.cookie.secure").getOrElse(true)) {
      Future(Redirect("https://" + req.domain + req.uri))
    } else {
      super.proceed(req)(f)
    }
  }

}
