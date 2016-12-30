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
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Try,Success,Failure}
import scala.util.matching.{Regex}
import java.io.{ByteArrayInputStream,FileInputStream}
import java.text.SimpleDateFormat
import java.util.{Calendar,TimeZone}
import javax.inject._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.{AmazonS3,AmazonS3Client}
import com.amazonaws.services.s3.model.{ObjectMetadata,PutObjectRequest}
import io.swagger.annotations._
import org.slf4j.{Logger,LoggerFactory}

@Singleton
@Api(value = "upload", description = "upload services", consumes = "multipart/form-data")
class UploadController @Inject() (configuration: play.api.Configuration) extends Controller {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  private val ImageFileNameRegExString:String = "^image([0-9]{4})([0-9]{2})([0-9]{2})s?([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{3}).jpg$"
  private val ImageFileNameRegEx:Regex = ImageFileNameRegExString.r

  private def putCurrentTextFile( s3Client:AmazonS3Client,
                                  bucketName:String,
                                  folderName:String,
                                  year:Int,
                                  month:Int,
                                  day:Int,
                                  hours:Int,
                                  minutes:Int,
                                  seconds:Int):Boolean = {
    val calendar:Calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"))
    calendar.set(year.toInt,month.toInt-1,day.toInt,hours.toInt,minutes.toInt,seconds.toInt)
    val formatter:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    formatter.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
    val dateString:String = formatter.format(calendar.getTime)
    val stringObjectMetadata:ObjectMetadata = new ObjectMetadata()
    stringObjectMetadata.setContentType("text/plain")
    stringObjectMetadata.setContentLength(dateString.size)
    Try(s3Client.putObject(new PutObjectRequest(bucketName, s"$folderName/current.txt", new ByteArrayInputStream(dateString.getBytes("UTF-8")), stringObjectMetadata))) match {
      case Success(x) => true
      case Failure(ex) => {
        m_log.error("Failed to save current.txt to S3",ex)
        false
      }
    }
  }

  private def putImage( s3Client:AmazonS3Client,
                        putObjectRequest:PutObjectRequest):Boolean = {
    Try(s3Client.putObject(putObjectRequest)) match {
      case Success(x) => true
      case Failure(ex) => {
        m_log.error("Failed to save image to S3",ex)
        false
      }
    }
  }

  @ApiOperation(value = "upload file", nickname="upload_file", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "my_file", value = "filex", required = false, dataType = "file", paramType = "formData")))
  def fileUpload(fileName:String) = Action { implicit request =>

    (configuration.getString("aws.s3.bucket"),configuration.getString("aws.s3.folder")) match {
      case (Some(bucketName),Some(folderName)) => {
        m_log.info(request.path)
        m_log.info(request.method)
        m_log.info(request.contentType.getOrElse("[ UNKNOWN ]"))
        m_log.info(request.body.getClass.toString)
        fileName match {
          case ImageFileNameRegEx(year,month,day,hours,minutes,seconds,milliseconds) => {
            m_log.info(s"year = $year")
            m_log.info(s"month = $month")
            m_log.info(s"day = $day")
            m_log.info(s"hours = $hours")
            m_log.info(s"minutes = $minutes")
            m_log.info(s"seconds = $seconds")
            m_log.info(s"milliseconds = $milliseconds")
            request.body match {
              case AnyContentAsMultipartFormData(mdf) => {
                mdf.file("my_file").map { myFile =>
                  val objectMetadata:ObjectMetadata = new ObjectMetadata()
                  myFile.contentType.map(contentType => objectMetadata.setContentType(contentType))
                  val s3Client:AmazonS3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/$year-$month-$day/$fileName", new FileInputStream(myFile.ref.file), objectMetadata))
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/current.jpg", new FileInputStream(myFile.ref.file), objectMetadata))
                  putCurrentTextFile(s3Client,bucketName,folderName,year.toInt,month.toInt,day.toInt,hours.toInt,minutes.toInt,seconds.toInt)
                }
              }
              case AnyContentAsRaw(rawBuffer) => {
                val objectMetadata:ObjectMetadata = new ObjectMetadata()
                request.contentType.map(contentType => objectMetadata.setContentType(contentType))
                objectMetadata.setContentLength(rawBuffer.size)
                val s3Client:AmazonS3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)
                rawBuffer.asBytes().map(buffer => {
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/$year-$month-$day/$fileName", new ByteArrayInputStream(buffer.toArray), objectMetadata))
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/current.jpg", new ByteArrayInputStream(buffer.toArray), objectMetadata))
                  putCurrentTextFile(s3Client,bucketName,folderName,year.toInt,month.toInt,day.toInt,hours.toInt,minutes.toInt,seconds.toInt)
                })
              }
              case _ => m_log.info("UNKNOWN body type")
            }
          }
          case _ => m_log.info("invalid file name")
        }
        Ok
      }
      case _ => ServiceUnavailable
    }
  }
}
