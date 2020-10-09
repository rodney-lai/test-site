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

import cats.effect.Blocker
import org.http4s.server.blaze.BlazeServerBuilder
import org.slf4j.LoggerFactory
import zio.console.putStrLn
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{ExitCode, Task, ZIO}
import com.google.inject.Guice
import com.rodneylai.services._

import scala.concurrent.duration._
import scala.language.postfixOps

object HttpApp extends CatsApp {
  private val log = LoggerFactory.getLogger(this.getClass.getName)

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    val injector = Guice.createInjector(
      new CacheServiceModule,
      new ImageServiceModule,
      new KafkaServiceModule,
      new MemCachedServiceModule,
      new RouterServiceModule
    )
    val routerService = injector.getInstance(classOf[RouterService])

    val ec = scala.concurrent.ExecutionContext.Implicits.global

    routerService.buildGraphQLInterpreter().flatMap({ graphQLInterpreter =>
      val server = for {
        blocker <- Blocker[Task]
        router = routerService.buildRouter(graphQLInterpreter,blocker)
        server <- BlazeServerBuilder[Task](ec)
          .withIdleTimeout(360 seconds)
          .withResponseHeaderTimeout(360 seconds)
          .bindHttp(8088, "0.0.0.0")
          .withHttpApp(router)
          .resource
      } yield {
        server
      }

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
