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

package controllers.services

import play.api.libs.json._
import play.api.Mode
import play.api.mvc._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure, Success, Try, Random}
import javax.inject.Inject
import io.swagger.annotations._
import jp.t2v.lab.play2.auth._
import org.jsoup.Jsoup
import org.jsoup.nodes._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth._

@Api(value = "/home", description = "home page services")
class home @Inject() (override val environment:play.api.Environment,override val configuration:play.api.Configuration,cache:play.api.cache.CacheApi,override val accountDao:AccountDao) extends Controller with OptionalAuthElement with AuthConfigImpl {

  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  private def scrapeImages(url:String,prefixUrl:String,excludeUrlList:Seq[String],list:Seq[String]):Seq[String] = {
    Try(Jsoup.connect(url).get) match {
      case Success(doc:Document) => {
        val linkList:Seq[String] = doc.select("a[href]").iterator().asScala.toSeq.map(_.attr("abs:href")).filter(linkUrl => linkUrl.startsWith(prefixUrl)).filter(linkUrl => !excludeUrlList.contains(linkUrl))

        linkList.foldLeft(list)((r,linkUrl) => scrapeImages(linkUrl,prefixUrl,excludeUrlList ++ linkList,r)) ++ doc.select("img[src]").iterator().asScala.toSeq.map(_.attr("abs:src"))
      }
      case Failure(ex) => {
        m_log.error(s"failed to get url [$url]",ex)
        list
      }
    }
  }

  private def doScrapeImages(url:String,prefixUrl:String):Seq[String] = {
    cache.getOrElse("home:image_list") {
      scrapeImages(url,prefixUrl,Seq[String](),Seq[String]())
    }
  }

  @ApiOperation(value = "get_scraped_images", notes = "returns list of scraped images", nickname="get_scraped_images", response = classOf[String], responseContainer="List", httpMethod = "GET")
  def get_scraped_images = Action {
    (configuration.getString("scrape.img.url"),configuration.getString("scrape.img.prefix.url")) match {
      case (Some(url),Some(prefixUrl)) => Ok(Json.toJson(Random.shuffle(doScrapeImages(url,prefixUrl))))
      case _ => Ok(Json.toJson(Seq[String]()))
    }
  }

  @ApiOperation(value = "who_am_i", notes = "returns who am i", nickname="who_am_i", response = classOf[String], httpMethod = "GET")
  def who_am_i = StackAction { implicit request =>
    loggedIn match {
      case Some(account) if (environment.mode != Mode.Dev) => Ok(Json.toJson(account.email))
      case Some(account) if (environment.mode == Mode.Dev) => Ok(Json.toJson(account.email)).withHeaders("Access-Control-Allow-Origin" -> "*")
      case None if (environment.mode != Mode.Dev) => Forbidden(Json.toJson("[unknown]"))
      case None if (environment.mode == Mode.Dev) => Forbidden(Json.toJson("[unknown]")).withHeaders("Access-Control-Allow-Origin" -> "*")
    }
  }

}
