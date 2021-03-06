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
import akka.stream.scaladsl._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure,Success,Try}
import java.io.InputStream
import javax.inject._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Regions}
import com.amazonaws.services.s3.{AmazonS3,AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{GetObjectRequest,ObjectMetadata,S3Object}
import org.slf4j.{Logger,LoggerFactory}

class Image @Inject() (configuration: play.api.Configuration) extends Controller {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def webcam = Action { implicit request =>
    (configuration.getString("aws.s3.bucket"),configuration.getString("aws.s3.folder")) match {
      case (Some(bucketName),Some(folderName)) => {
        val region:Regions = configuration.getString("aws.s3.region") match {
          case Some(regionName) => Regions.fromName(regionName)
          case None => Regions.US_EAST_1
        }
        val s3Client:AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(new DefaultAWSCredentialsProviderChain).build()
        val s3Object:S3Object = s3Client.getObject(new GetObjectRequest(bucketName, s"$folderName/current.jpg"))
        val objectDataStream:InputStream = s3Object.getObjectContent
        val dataContent:Source[akka.util.ByteString, Future[akka.stream.IOResult]] = StreamConverters.fromInputStream(() => objectDataStream)

        Ok.chunked(dataContent).as("image/jpeg")
      }
      case _ => NotFound
    }
  }

}
