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

import play.api._
import play.api.mvc._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.{Source}
import scala.util.{Failure, Success, Try}
import javax.inject.Inject
import com.github.rjeschke._
import jp.t2v.lab.play2.auth._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.amazon.s3._
import com.rodneylai.auth._
import com.rodneylai.database._
import com.rodneylai.stackc._
import com.rodneylai.util._

class Application @Inject() (s3Helper:S3Helper,infoHelper:InfoHelper,override val accountDao:AccountDao,override val trackingHelper:TrackingHelper)(implicit override val environment: play.api.Environment, override val configuration: play.api.Configuration) extends Controller with TrackingPageView with OptionalAuthElement with LoginLogout with AuthConfigImpl {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def index = StackAction { implicit request =>
    ExceptionHelper.checkForError(request,views.html.index(loggedIn))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  private def getReadMeHtml:Option[String] = {
    if (new java.io.File(environment.rootPath + "/README").exists) {
      val readme = Source.fromFile(environment.rootPath + "/README").getLines.mkString("\n")

      Some(txtmark.Processor.process("[$PROFILE$]: extended\n" + readme))
    } else if (new java.io.File(environment.rootPath + "/../README").exists) {
      val readme = Source.fromFile(environment.rootPath + "/../README").getLines.mkString("\n")

      Some(txtmark.Processor.process("[$PROFILE$]: extended\n" + readme))
    } else {
      None
    }
  }

  def about = StackAction { implicit request =>
    Ok(views.html.about(loggedIn,getReadMeHtml,infoHelper.getBuildDate))
  }

  def webcam = StackAction { implicit request =>
    s3Helper.getS3TextFiles(Array("current.txt","histogram.csv")) match {
      case Some(Array(Some(dateString),Some(histogramData))) => {
        Ok(views.html.webcam(loggedIn,dateString,histogramData))
      }
      case None => Ok(views.html.webcam_not_configured(loggedIn))
    }

  }

}
