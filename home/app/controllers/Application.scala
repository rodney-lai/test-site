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
import java.io.{BufferedReader,InputStream,InputStreamReader}
import javax.inject.Inject
import com.github.rjeschke._
import jp.t2v.lab.play2.auth._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Regions}
import com.amazonaws.services.s3.{AmazonS3,AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{GetObjectRequest,ObjectMetadata,S3Object}
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._
import com.rodneylai.database._
import com.rodneylai.stackc._
import com.rodneylai.util._

class Application @Inject() (infoHelper:InfoHelper,override val accountDao:AccountDao,override val trackingHelper:TrackingHelper)(implicit override val environment: play.api.Environment, override val configuration: play.api.Configuration) extends Controller with TrackingPageView with OptionalAuthElement with LoginLogout with AuthConfigImpl {
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

  private def getS3File(s3Client:AmazonS3,bucketName:String,key:String) = {
    Try(s3Client.getObject(new GetObjectRequest(bucketName, key))) match {
      case Success(s3Object:S3Object) => {
        val objectDataStream:InputStream = s3Object.getObjectContent
        val inputStreamReader:InputStreamReader = new InputStreamReader(objectDataStream)
        val bufferedReader:BufferedReader = new BufferedReader(inputStreamReader)
        val content:String = bufferedReader.readLine

        bufferedReader.close
        inputStreamReader.close
        objectDataStream.close
        content
      }
      case Failure(ex) =>
        m_log.error(s"Failed to read S3 file = $key",ex)
        ""
    }
  }

  def webcam = StackAction { implicit request =>
    (configuration.getString("aws.s3.bucket"),configuration.getString("aws.s3.folder")) match {
      case (Some(bucketName),Some(folderName)) => {
        val region:Regions = configuration.getString("aws.s3.region") match {
          case Some(regionName) => Regions.fromName(regionName)
          case None => Regions.US_EAST_1
        }
        val s3Client:AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(new DefaultAWSCredentialsProviderChain).build()
        val dateString:String = getS3File(s3Client,bucketName,s"$folderName/current.txt")
        val histogramData:String = getS3File(s3Client,bucketName,s"$folderName/histogram.csv")

        Ok(views.html.webcam(loggedIn,dateString,histogramData))
      }
      case _ => Ok(views.html.webcam_not_configured(loggedIn))
    }

  }

}
