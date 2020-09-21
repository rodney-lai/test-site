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

import org.scalatest.Assertion
import org.scalatest.wordspec._
import org.scalatest.matchers.should.Matchers
import org.mockito.ArgumentMatchers._
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.mockito.Mockito._
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

  }

}
