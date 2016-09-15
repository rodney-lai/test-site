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
import scala.io.{Source}
import scala.util.{Failure, Success, Try}
import java.io.{BufferedReader,InputStream,InputStreamReader}
import javax.inject.Inject
import com.github.rjeschke._
import jp.t2v.lab.play2.auth._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.{AmazonS3,AmazonS3Client}
import com.amazonaws.services.s3.model.{GetObjectRequest,ObjectMetadata,S3Object}
import com.rodneylai.auth._
import com.rodneylai.database._
import com.rodneylai.stackc._
import com.rodneylai.util._

class Application @Inject() (infoHelper:InfoHelper,override val accountDao:AccountDao,override val trackingHelper:TrackingHelper)(implicit override val environment: play.api.Environment, override val configuration: play.api.Configuration) extends Controller with TrackingPageView with OptionalAuthElement with LoginLogout with AuthConfigImpl {

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
    (configuration.getString("aws.s3.bucket"),configuration.getString("aws.s3.folder")) match {
      case (Some(bucketName),Some(folderName)) => {
        val s3Client:AmazonS3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)
        val s3Object:S3Object = s3Client.getObject(new GetObjectRequest(bucketName, s"$folderName/current.txt"))
        val objectDataStream:InputStream = s3Object.getObjectContent
        val inputStreamReader:InputStreamReader = new InputStreamReader(objectDataStream)
        val bufferedReader:BufferedReader = new BufferedReader(inputStreamReader)
        val dateString:String = bufferedReader.readLine

        bufferedReader.close
        inputStreamReader.close
        objectDataStream.close
        Ok(views.html.webcam(loggedIn,dateString))
      }
      case _ => Ok(views.html.webcam_not_configured(loggedIn))
    }

  }

}
