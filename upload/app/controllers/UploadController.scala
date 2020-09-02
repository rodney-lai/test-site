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

import play.api.mvc._
import scala.util.{Try,Success,Failure}
import scala.util.matching.{Regex}
import java.io.{ByteArrayInputStream,FileInputStream}
import java.text.SimpleDateFormat
import java.util.{Calendar,TimeZone}
import javax.inject._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Regions}
import com.amazonaws.services.s3.{AmazonS3,AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{ObjectMetadata,PutObjectRequest}
import io.swagger.annotations._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs
import org.slf4j.{Logger,LoggerFactory}

@Singleton
@Api(value = "upload", description = "upload services", consumes = "multipart/form-data")
class UploadController @Inject() (
  configuration: play.api.Configuration,
  cc: ControllerComponents
) extends AbstractController(cc) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  private val ImageFileNameRegExString:String = "^image([0-9]{4})([0-9]{2})([0-9]{2})s?([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{3}).jpg$"
  private val ImageFileNameRegEx:Regex = ImageFileNameRegExString.r

  private def putCurrentTextFile( s3Client:AmazonS3,
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

  private def putImage( s3Client:AmazonS3,
                        putObjectRequest:PutObjectRequest):Boolean = {
    Try(s3Client.putObject(putObjectRequest)) match {
      case Success(x) => true
      case Failure(ex) => {
        m_log.error("Failed to save image to S3",ex)
        false
      }
    }
  }

  private def calcHistogram(mat:Mat):String = {
    val width = mat.cols()
    val height = mat.rows()
    val blocks = 4
    val feature:Array[Int] = Array.fill[Int](blocks * blocks * blocks)(0)

    for {
      y <- 0 to height - 1
      x <- 0 to width - 1
    } yield {
      val b:Int = mat.ptr(y,x).get(0) & 0xff
      val g:Int = mat.ptr(y,x).get(1) & 0xff
      val r:Int = mat.ptr(y,x).get(2) & 0xff
      val ridx:Int = scala.math.floor(r.toFloat/(256.0f/blocks.toFloat)).toInt
      val gidx:Int = scala.math.floor(g.toFloat/(256.0f/blocks.toFloat)).toInt
      val bidx:Int = scala.math.floor(b.toFloat/(256.0f/blocks.toFloat)).toInt
      val idx:Int = bidx + gidx * blocks + ridx * blocks * blocks
      feature.update(idx,feature(idx) + 1)
    }
    feature.map(x => x.toFloat/(height.toFloat * width.toFloat)).map(x => f"$x%.4f").mkString(",")
  }

  private def doCalcHistogram(filePath:String):String = {
    val mat = opencv_imgcodecs.imread(filePath)
    val histogram:String = calcHistogram(mat)

    mat.release()
    histogram
  }

  private def doCalcHistogram(buffer:akka.util.ByteString):String = {
    val mat = opencv_imgcodecs.imdecode(new Mat(buffer.toArray,true),opencv_imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
    val histogram:String = calcHistogram(mat)

    mat.release()
    histogram
  }

  private def putHistogram( s3Client:AmazonS3,
                            bucketName:String,
                            key:String,
                            histogram:String):Boolean = {
    val stringObjectMetadata:ObjectMetadata = new ObjectMetadata()

    stringObjectMetadata.setContentType("text/csv")
    stringObjectMetadata.setContentLength(histogram.size)
    Try(s3Client.putObject(new PutObjectRequest(bucketName, key, new ByteArrayInputStream(histogram.getBytes("UTF-8")), stringObjectMetadata))) match {
      case Success(x) => true
      case Failure(ex) => {
        m_log.error("Failed to save histogram to S3",ex)
        false
      }
    }
  }

  private def createHistogram(s3Client:AmazonS3,
                              bucketName:String,
                              keys:Array[String],
                              filePath:String):Boolean = {
    Try (doCalcHistogram(filePath)) match {
      case Success(histogram) => keys.map(key => putHistogram(s3Client,bucketName,key,histogram)).reduce(_ | _)
      case Failure(ex) => {
        m_log.error("Failed to calc histogram",ex)
        false
      }
    }
  }

  private def createHistogram(s3Client:AmazonS3,
                              bucketName:String,
                              keys:Array[String],
                              buffer:akka.util.ByteString):Boolean = {
    Try (doCalcHistogram(buffer)) match {
      case Success(histogram) => keys.map(key => putHistogram(s3Client,bucketName,key,histogram)).reduce(_ | _)
      case Failure(ex) => {
        m_log.error("Failed to calc histogram",ex)
        false
      }
    }
  }

  @ApiOperation(value = "upload file", nickname = "upload_file", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "my_file", value = "filex", required = false, dataType = "file", paramType = "formData")))
  def fileUpload(fileName:String) = Action { implicit request =>

    (configuration.getOptional[String]("aws.s3.bucket"),configuration.getOptional[String]("aws.s3.folder")) match {
      case (Some(bucketName),Some(folderName)) => {
        val region:Regions = configuration.getOptional[String]("aws.s3.region") match {
          case Some(regionName) => Regions.fromName(regionName)
          case None => Regions.US_EAST_1
        }

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
            val mb = 1024*1024
            val runtime = Runtime.getRuntime
            m_log.info("memory")
            m_log.info(s" used = ${(runtime.totalMemory - runtime.freeMemory) / mb}mb")
            m_log.info(s" free = ${runtime.freeMemory / mb}mb")
            m_log.info(s" total = ${runtime.totalMemory / mb}mb")
            m_log.info(s" max = ${runtime.maxMemory / mb}mb")
            request.body match {
              case AnyContentAsMultipartFormData(mdf) => {
                mdf.file("my_file").map { myFile =>
                  val objectMetadata:ObjectMetadata = new ObjectMetadata()
                  myFile.contentType.map(contentType => objectMetadata.setContentType(contentType))
                  val s3Client:AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(new DefaultAWSCredentialsProviderChain).build()
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/$year-$month-$day/$fileName", new FileInputStream(myFile.ref.path.toFile()), objectMetadata))
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/current.jpg", new FileInputStream(myFile.ref.path.toFile()), objectMetadata))
                  putCurrentTextFile(s3Client,bucketName,folderName,year.toInt,month.toInt,day.toInt,hours.toInt,minutes.toInt,seconds.toInt)
                  createHistogram(s3Client,bucketName,Array(s"$folderName/$year-$month-$day/${fileName.replaceAll(".jpg","_histogram.csv")}",s"$folderName/histogram.csv"),myFile.ref.path.toFile().getAbsolutePath())
                }
              }
              case AnyContentAsRaw(rawBuffer) => {
                val objectMetadata:ObjectMetadata = new ObjectMetadata()
                request.contentType.map(contentType => objectMetadata.setContentType(contentType))
                objectMetadata.setContentLength(rawBuffer.size)
                val s3Client:AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(new DefaultAWSCredentialsProviderChain).build()
                rawBuffer.asBytes().map(buffer => {
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/$year-$month-$day/$fileName", new ByteArrayInputStream(buffer.toArray), objectMetadata))
                  putImage(s3Client,new PutObjectRequest(bucketName, s"$folderName/current.jpg", new ByteArrayInputStream(buffer.toArray), objectMetadata))
                  putCurrentTextFile(s3Client,bucketName,folderName,year.toInt,month.toInt,day.toInt,hours.toInt,minutes.toInt,seconds.toInt)
                  createHistogram(s3Client,bucketName,Array(s"$folderName/$year-$month-$day/${fileName.replaceAll(".jpg","_histogram.csv")}",s"$folderName/histogram.csv"),buffer)
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
