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

import play.api.Mode
import play.api.mvc._
import javax.inject.Inject
import jp.t2v.lab.play2.auth._
import com.rodneylai.auth._

class Test @Inject() (override val accountDao:AccountDao)(implicit environment: play.api.Environment) extends Controller with OptionalAuthElement with AuthConfigImpl {

  def isAdmin(accountOption:Option[Account]):Boolean = {
    accountOption match {
      case Some(account) if (account.role == Role.Administrator) => true
      case _ => false
    }
  }

  def throw_exception = StackAction { implicit request =>
    if ((environment.mode == Mode.Dev) || (isAdmin(loggedIn))) {
      val x:Integer = 10
      val y:Integer = 0
      val z:Integer = x/y
      Ok(views.html.test(loggedIn,"throw exception"))
    } else {
      Redirect(controllers.routes.Application.index).flashing("error" -> "handler_not_found")
    }
  }

  def test_val(x:Integer) = StackAction { implicit request =>
    if ((environment.mode == Mode.Dev) || (isAdmin(loggedIn))) {
      Ok(views.html.test(loggedIn,"my value is " + x.toString))
    } else {
      Redirect(controllers.routes.Application.index).flashing("error" -> "handler_not_found")
    }
  }

}
