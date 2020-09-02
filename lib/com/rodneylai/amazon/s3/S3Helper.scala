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

package com.rodneylai.amazon.s3

import scala.io.{BufferedSource}
import scala.util.{Failure, Success, Try}
import java.io.{InputStream}
import javax.inject.{Inject,Singleton}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Regions}
import com.amazonaws.services.s3.{AmazonS3,AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{AmazonS3Exception,GetObjectRequest,S3Object}
import com.google.inject.AbstractModule
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.util._

@Singleton
class S3Helper @Inject() (configHelper:ConfigHelper) {
  private val       m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  private def getS3TextFile(s3Client:AmazonS3,bucketName:String,key:String):Option[String] = {
    Try(s3Client.getObject(new GetObjectRequest(bucketName, key))) match {
      case Success(s3Object:S3Object) => {
        val objectDataStream:InputStream = s3Object.getObjectContent

        try {
          val bufferedSource:BufferedSource = new BufferedSource(objectDataStream)

          Some(bufferedSource.getLines().mkString("\n"))
        } catch {
          case ex:Exception =>
            m_log.error(s"Failed to read S3 file = $key",ex)
            None
        } finally {
          objectDataStream.close
        }
      }
      case Failure(ex:AmazonS3Exception) =>
        if (ex.getStatusCode() == 404) {
          m_log.warn(s"Failed to find S3 file = $key")
        } else {
          m_log.error(s"Failed to read S3 file = $key",ex)
        }
        None
      case Failure(ex) =>
        m_log.error(s"Failed to read S3 file = $key",ex)
        None
    }
  }

  def getS3TextFiles(keys:Array[String]):Option[Array[Option[String]]] = {
    (configHelper.getString("aws.s3.bucket"),configHelper.getString("aws.s3.folder")) match {
      case (Some(bucketName),Some(folderName)) => {
        val region:Regions = configHelper.getString("aws.s3.region") match {
          case Some(regionName) => Regions.fromName(regionName)
          case None => Regions.US_EAST_1
        }
        val s3Client:AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(new DefaultAWSCredentialsProviderChain).build()

        Some(keys.map(key => getS3TextFile(s3Client,bucketName,s"$folderName/$key")))
      }
      case _ => None
    }
  }

}

class S3HelperModule extends AbstractModule {
  override def configure() = {
    bind(classOf[S3Helper]).asEagerSingleton
  }
}
