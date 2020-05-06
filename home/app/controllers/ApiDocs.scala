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

package controllers

import play.api.Mode
import play.api.mvc._
import javax.inject.Inject
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import jp.t2v.lab.play2.auth._
import com.rodneylai.auth._
import com.rodneylai.security._
import com.rodneylai.stackc._

class ApiDocs @Inject() (override val environment:play.api.Environment,override val configuration:play.api.Configuration,deadbolt: DeadboltActions, actionBuilder: ActionBuilders,override val accountDao:AccountDao) extends Controller with OptionalAuthElement with AuthConfigImpl with RequireSSL {

  def index = if (environment.mode == Mode.Dev) {
    StackAction { implicit request =>
      Ok(views.html.api_docs.index())
    }
  } else {
    StackAction { implicit request =>
      loggedIn match {
        case Some(account) => Redirect(controllers.routes.Developer.api)
        case None => Redirect(controllers.routes.Application.index)
      }
    }
  }

}
