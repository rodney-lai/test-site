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

import cats.effect.Blocker
import org.http4s.Request
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sLiteralsSyntax
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.wordspec._
import org.scalatest.matchers.should.Matchers
import scala.concurrent.Future
import zio.interop.catz._
import zio.{Runtime, Task}
import zio.stream.ZStream

class RouterServiceTests extends AsyncWordSpec
  with Matchers
  with IdiomaticMockito
  with ArgumentMatchersSugar {

  "RouterService" can {

    "router" should {
      val images = Seq(Image(
        "theImgUrl",
        "theImgTitle",
        "theTitle",
        "theLinkUrl"
      ))

      val dsl = new Http4sDsl[Task]{}
      import dsl._

      def TestFixture(
        request: Request[Task],
        expectedResult: String
      ): Future[Assertion] = {
        val imageService = mock[ImageService]
        val kafkaService = mock[KafkaService]
        val routerService = spy(new RouterServiceImpl(imageService,kafkaService))

        when(
          imageService.getImages()
        ).thenReturn(Task.succeed(images))

        when(
          kafkaService.getWebCamStream()
        ).thenReturn(ZStream.empty)

        doReturn("theBuildDate").when(routerService).buildDate

        implicit val runtime = Runtime.default

        val testResult = routerService.buildGraphQLInterpreter().flatMap { graphQLInterpreter =>
          val responseResource = for {
            blocker <- Blocker[Task]
            router = routerService.buildRouter(graphQLInterpreter, blocker)(runtime)
            response = router.run(request)
          } yield {
            response
          }
          for {
            response <- responseResource.use { responseTask => responseTask }
            body <- response.body.compile.fold("")((body,charValue) =>
              body + charValue.toChar
            )
          } yield {
            body shouldBe expectedResult
          }
        }

        Runtime.default.unsafeRunToFuture(
          testResult
        )
      }

      "return build date on version REST endpoint" in {
        TestFixture(
          Request(method = GET, uri = uri"/version"),
          """{"buildDate":"theBuildDate"}"""
        )
      }

      "return images on images REST endpoint" in {
        TestFixture(
          Request(method = GET, uri = uri"/images"),
          """[{"imgUrl":"theImgUrl","imgTitle":"theImgTitle","title":"theTitle","linkUrl":"theLinkUrl"}]"""
        )
      }

      "return build date for version GraphQL query" in {
        TestFixture(
          Request(method = POST, uri = uri"/graphql")
            .withBodyStream(
              fs2.Stream.emits("""{"query":"query { version { buildDate } }"}""".getBytes)
            ),
          """{"data":{"version":{"buildDate":"theBuildDate"}}}"""
        )
      }

      "return images title for images title GraphQL query" in {
        TestFixture(
          Request(method = POST, uri = uri"/graphql")
            .withBodyStream(
              fs2.Stream.emits("""{"query":"query { images { title } }"}""".getBytes)
            ),
          """{"data":{"images":[{"title":"theTitle"}]}}"""
        )
      }

      "return images for images all fields GraphQL query" in {
        TestFixture(
          Request(method = POST, uri = uri"/graphql")
            .withBodyStream(
              fs2.Stream.emits("""{"query":"query { images { title linkUrl imgTitle imgTitle } }"}""".getBytes)
            ),
          """{"data":{"images":[{"title":"theTitle","linkUrl":"theLinkUrl","imgTitle":"theImgTitle"}]}}"""
        )
      }

    }

  }

}
