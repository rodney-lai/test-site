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

import caliban.{CalibanError, GraphQLInterpreter, Http4sAdapter, RootResolver}
import caliban.GraphQL.graphQL
import caliban.ResponseValue._
import cats.data.Kleisli
import cats.effect.Blocker
import com.google.inject.{AbstractModule, Inject}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.middleware.{CORS, Timeout}
import org.http4s.server.staticcontent._
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.io
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import zio.{IO, Runtime, Task, ZEnv}
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.stream.ZStream

trait RouterService {
  def buildGraphQLInterpreter(): IO[CalibanError.ValidationError, GraphQLInterpreter[Any, CalibanError]]
  def buildRouter(
    interpreter: GraphQLInterpreter[Any, CalibanError],
    blocker: Blocker
  )(implicit runtime: Runtime[ZEnv]): Kleisli[Task, Request[Task], Response[Task]]
}

class RouterServiceImpl @Inject() (
  imageService: ImageService,
  kafkaService: KafkaService
) extends RouterService {
  private val log = LoggerFactory.getLogger(this.getClass.getName)

  lazy val buildDate: String = {
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
  case class Mutations(empty: String)
  case class Subscriptions(webcamUpdated: ZStream[Any, Throwable, Option[String]])

  def buildRouter(
    interpreter: GraphQLInterpreter[Any, CalibanError],
    blocker: Blocker
  )(implicit runtime: Runtime[ZEnv]): Kleisli[Task, Request[Task], Response[Task]] = {
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

    val graphqlService = Http4sAdapter.makeHttpService(
      interpreter.mapError {
        case err =>
          log.error("[graphql_failure]", err)
          err
      }
    )

    val wsGraphqlService = Http4sAdapter.makeWebSocketService(
      interpreter.mapError {
        case err =>
          log.error("[ws_graphql_failure]", err)
          err
      }
    )

    Router[Task](
      "/" -> infoRESTService,
      "/" -> CORS(fileService(FileService.Config("./files", blocker))),
      "/" -> CORS(Timeout(360 seconds)(imageRESTService)),
      "/graphql" -> CORS(Timeout(360 seconds)(graphqlService)),
      "/ws/graphql"  -> CORS(wsGraphqlService)
    ).orNotFound
  }

  def buildGraphQLInterpreter(): IO[CalibanError.ValidationError, GraphQLInterpreter[Any, CalibanError]] = {
    val queries = Queries(
      Version(buildDate),
      () => imageService.getImages()
    )
    val mutations = Mutations("nothing here")
    val subscriptions = Subscriptions(kafkaService.getWebCamStream())

    val api = graphQL(RootResolver(queries,mutations,subscriptions))

    api.interpreter
  }

}

class RouterServiceModule extends AbstractModule {
  override def configure() = {
    bind(classOf[RouterService]).to(classOf[RouterServiceImpl])
  }
}
