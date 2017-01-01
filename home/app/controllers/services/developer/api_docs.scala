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

package controllers.services.developer

import play.api.cache._
import play.api.libs.json._
import play.api.Mode
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.concurrent.duration._
import javax.inject.Inject
import be.objectify.deadbolt.scala.{ActionBuilders,DeadboltActions}
import io.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.security._

class api_docs @Inject() (override val environment:play.api.Environment,override val configuration:play.api.Configuration,deadbolt: DeadboltActions, actionBuilder: ActionBuilders,override val accountDao:AccountDao) extends Controller with AuthElement with AuthConfigImpl {

  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)
  private val m_apiHelpController = new controllers.ApiHelpController

  def getResources = if (environment.mode == Mode.Dev) {
    m_apiHelpController.getResources
  } else {
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
      deadbolt.Restrict(List(Array("developer")), new DefaultDeadboltHandler(Some(loggedIn)))() { authRequest =>
        m_apiHelpController.getResources.apply(request)
      }.apply(request)
    }
  }

  def getResource(path: String) = if (environment.mode == Mode.Dev) {
    m_apiHelpController.getResource(path)
  } else {
    AsyncStack(AuthorityKey -> Role.Administrator) { implicit request =>
      deadbolt.Restrict(List(Array("developer")), new DefaultDeadboltHandler(Some(loggedIn)))() { authRequest =>
        m_apiHelpController.getResource(path).apply(request)
      }.apply(request)
    }
  }

}
