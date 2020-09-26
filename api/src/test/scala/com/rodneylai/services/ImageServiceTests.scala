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

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.wordspec._
import org.scalatest.matchers.should.Matchers
import pureconfig._
import pureconfig.generic.auto._
import scala.concurrent.Future
import zio.{Runtime, Task}

class ImageServiceTests extends AsyncWordSpec
  with Matchers
  with IdiomaticMockito
  with ArgumentMatchersSugar {

  "ImageService" can {

    "getImages" should {
      val images = Seq(Image(
        "theImgUrl",
        "theImgTitle",
        "theTitle",
        "theLinkUrl"
      ))

      def TestFixture(
        cacheValue: Option[String],
        config: String = """{"url": "theUrl", "prefix" : {"url": "thePrefixUrl"}}"""
      )(test: (ImageServiceImpl) => Task[Assertion]): Future[Assertion] = {
        val cacheService = mock[CacheService]
        val imageService = spy(new ImageServiceImpl(cacheService))

        when(
          cacheService.get(anyString)
        ).thenReturn(cacheValue)
        when(
          cacheService.set(anyString, anyString)
        ).thenReturn(Task.succeed(true))

        doReturn(Task.succeed(images)).when(imageService).doScrapeImages(anyString, anyString)

        doReturn(
          ConfigSource.string(config).load[ScrapeImgConfig]
        ).when(imageService).loadConfig()

        Runtime.default.unsafeRunToFuture(
          test(imageService)
        )
      }

      "return no images for invalid json in cache" in {
        TestFixture(
          Some("invalid json string")
        ) { imageService =>
          for {
            result <- imageService.getImages()
          } yield {
            verify(imageService, times(0)).doScrapeImages(anyString, anyString)
            result.size shouldBe 0
          }
        }
      }

      "return images from cache" in {
        TestFixture(
          Some("""[{"imgUrl":"theImgUrl","imgTitle":"theImgTitle","title":"theTitle","linkUrl":"theLinkUrl"}]""")
        ) { imageService =>
          for {
            result <- imageService.getImages()
          } yield {
            verify(imageService, times(0)).doScrapeImages(anyString, anyString)
            result shouldBe images
          }
        }
      }

      "return scraped images if images not in cache" in {
        TestFixture(
          None
        ) { imageService =>
          for {
            result <- imageService.getImages()
          } yield {
            verify(imageService, times(1)).doScrapeImages(anyString, anyString)
            result shouldBe images
          }
        }
      }

      "return no images for invalid config" in {
        TestFixture(
          None,
          """{"url_INVALID_KEY": "theUrl", "prefix" : {"url": "thePrefixUrl"}}"""
        ) { imageService =>
          for {
            result <- imageService.getImages()
          } yield {
            verify(imageService, times(0)).doScrapeImages(anyString, anyString)
            result.size shouldBe 0
          }
        }
      }
    }

    "scrapeImages" can {
      val baseUrl = "http://test.rodneylai.com"
      val prefixBase = "prefixUrl"
      val prefixUrl = s"${baseUrl}/${prefixBase}"

      def TestFixture(
        html: Map[String,String] = Map[String,String]()
      )(test: (ImageServiceImpl) => Task[Assertion]): Future[Assertion] = {
        val cacheService = mock[CacheService]
        val imageService = spy(new ImageServiceImpl(cacheService))

        doThrow(
          new org.jsoup.UncheckedIOException("something bad happened")
        ).when(imageService).getDocument(anyString)
        html.foreach { case (key, value) =>
          doReturn({
            val doc = Jsoup.parse(value)
            doc.setBaseUri(baseUrl)
            doc
          }).when(imageService).getDocument(key)
        }

        Runtime.default.unsafeRunToFuture(
          test(imageService)
        )
      }

      "return no images if html url is invalid" in {
        TestFixture() { imageService =>
          for {
            result <- imageService.doScrapeImages(s"${baseUrl}/url1",prefixUrl)
          } yield {
            result should be (empty)
          }
        }
      }

      "find images in html" in {
        TestFixture(
          Map(
            (s"${baseUrl}/url1" -> s"""<html><head><title>title1</title></head><body><img src="imgSrc1" title="imgTitle1" alt="imgAlt1" /><a href="${prefixBase}/url2">test link</a></body></html>"""),
            (s"${prefixUrl}/url2" -> """<html><head><title>title2</title></head><body><img src="imgSrc2" title="imgTitle2" alt="imgAlt2" /></body></html>""")
          )
        ) { imageService =>
          for {
            result <- imageService.doScrapeImages(s"${baseUrl}/url1",prefixUrl)
          } yield {
            result should have size (2)
            result should contain (
              Image(s"${baseUrl}/imgSrc1","imgAlt1","title1",s"${baseUrl}/url1")
            )
            result should contain (
              Image(s"${baseUrl}/imgSrc2","imgAlt2","title2",s"${prefixUrl}/url2")
            )
          }
        }
      }

    }

  }

}
