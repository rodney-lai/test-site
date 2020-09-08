/**
 *
 * Copyright (c) 2020 Rodney S.K. Lai
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

package com.rodneylai.services

import java.time.{Duration, Instant}
import com.google.inject.{AbstractModule, Inject}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.jsoup.Jsoup
import org.jsoup.nodes._
import org.slf4j.LoggerFactory
import pureconfig._
import pureconfig.generic.auto._
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import zio.Task

case class Image(
  imgUrl: String,
  imgTitle: String,
  title: String,
  linkUrl: String
)

trait ImageService {
  def getImages(): Task[Seq[Image]]
}

class ImageServiceImpl @Inject() (
  cacheService: CacheService
) extends ImageService {
  private val log = LoggerFactory.getLogger(this.getClass.getName)

  case class ScrapeImgPrefixConfig(url: String)
  case class ScrapeImgConfig(url: String, prefix: ScrapeImgPrefixConfig)

  private def scrapeImages(url:String,prefixUrl:String,excludeUrlList:Seq[String],list:Seq[Image]):Seq[Image] = {
    if(url != s"${prefixUrl}/") log.debug(url)
    Try(Jsoup.connect(url).get) match {
      case Success(doc:Document) =>
        val linkList:Seq[String] = doc.select("a[href]").iterator().asScala.toSeq.map(_.attr("abs:href")).filter(linkUrl => linkUrl.startsWith(prefixUrl)).filter(linkUrl => !excludeUrlList.contains(linkUrl))

        linkList.foldLeft(list)((r,linkUrl) =>
          scrapeImages(linkUrl,prefixUrl,excludeUrlList ++ linkList,r)
        ) ++ doc.select("img[src]").iterator().asScala.toSeq.map(
          img => Image(img.attr("abs:src"),img.attr("alt"),doc.title(),url)
        )
      case Failure(ex) =>
        log.error(s"failed to get url [$url]",ex)
        list
    }
  }

  private def doScrapeImages(url:String,prefixUrl:String): Task[Seq[Image]] = Task {
    val startInstant = Instant.now
    val images = scrapeImages(url,prefixUrl,Seq[String](),Seq[Image]())
    log.info(s"doScrapeImages[duration=${Duration.between(startInstant,Instant.now)}][count=${images.size}]")
    images
  }

  def getImages(): Task[Seq[Image]] = {
    cacheService.get("test_api:images") match {
      case Some(imagesJsonString) =>
        val images = decode[Seq[Image]](imagesJsonString) match {
          case Right(images) =>
            images
          case Left(failure) =>
            log.error("getImages[invalid_json]",failure)
            Seq[Image]()
        }
        Task.succeed(images)
      case None =>
        ConfigSource.default.at("scrape.img").load[ScrapeImgConfig] match {
          case Right(scrapeImgConfig) =>
            for {
              images <- doScrapeImages(scrapeImgConfig.url,scrapeImgConfig.prefix.url)
              result <- cacheService.set("test_api:images",images.asJson.toString)
            } yield {
              if(!result) log.error("getImages[failed_to_update_memcached]")
              images
            }
          case Left(ex) =>
            log.error("getImages[invalid_config]",ex)
            Task.succeed(Seq[Image]())
        }
    }
  }

}

class ImageServiceModule extends AbstractModule {
  override def configure() = {
    bind(classOf[ImageService]).to(classOf[ImageServiceImpl])
  }
}
