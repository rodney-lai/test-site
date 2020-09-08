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

package com.rodneylai

import caliban.{Http4sAdapter, RootResolver}
import caliban.GraphQL.graphQL
import caliban.ResponseValue._
import cats.effect.Blocker
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Timeout}
import org.http4s.server.staticcontent._
import org.slf4j.LoggerFactory
import zio.console.putStrLn
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{ExitCode, Task, ZIO}
import com.google.inject.Guice
import com.rodneylai.services._

import scala.concurrent.duration._
import scala.io
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object HttpApp extends CatsApp {
  private val log = LoggerFactory.getLogger(this.getClass.getName)

  val buildDate: String = {
    Try(io.Source.fromFile("./BUILD_DATE").getLines().mkString("\n")) match {
      case Success(result) => result
      case Failure(ex) => "unknown"
    }
  }

  case class Version(
    buildDate: String,
  )

  case class Queries(
    version: Version,
    images: () => Task[Seq[Image]],
  )

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    val injector = Guice.createInjector(
      new CacheServiceModule,
      new ImageServiceModule,
      new MemCachedServiceModule
    )
    val imageService = injector.getInstance(classOf[ImageService])

    val dsl = new Http4sDsl[Task]{}
    import dsl._

    val infoRESTService = HttpRoutes.of[Task] {
      case GET -> Root / "version" =>
        Ok(Version(buildDate).asJson)
    }

    val imageRESTService = HttpRoutes.of[Task] {
      case GET -> Root / "images" =>
        Ok(imageService.getImages().map(_.asJson))
    }

    val queries = Queries(
      Version(buildDate),
      () => imageService.getImages()
    )

    val interpreter = graphQL(RootResolver(queries)).interpreter

    val ec = scala.concurrent.ExecutionContext.Implicits.global

    interpreter
      .flatMap({
        interpreter =>
          val server = for {
            blocker <- Blocker[Task]
            server <- BlazeServerBuilder[Task](ec)
              .withIdleTimeout(360 seconds)
              .withResponseHeaderTimeout(360 seconds)
              .bindHttp(8088, "0.0.0.0")
              .withHttpApp(
                Router[Task](
                  "/" -> infoRESTService,
                  "/" -> CORS(fileService(FileService.Config("./files", blocker))),
                  "/" -> CORS(Timeout(360 seconds)(imageRESTService)),
                  "/graphql" -> CORS(Timeout(360 seconds)(Http4sAdapter.makeHttpService(
                    interpreter.mapError {
                      case err =>
                        log.error("[graphql_failure]", err)
                        err
                    }
                  )))
                ).orNotFound
              )
              .resource
          } yield server

          server
            .toManaged
            .useForever
      })
      .catchAll(err => {
        log.error("[unexpected_failure]",err)
        putStrLn(err.toString)
      })
      .as(ExitCode.success)
  }
}
