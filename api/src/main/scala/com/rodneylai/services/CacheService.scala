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

import com.google.inject.{AbstractModule, Inject}
import net.spy.memcached.internal.{OperationFuture, OperationCompletionListener}
import org.slf4j.LoggerFactory
import scala.collection.mutable.Map
import zio.{Task, ZIO}

trait CacheService {
  def get(key: String): Option[String]
  def set(key: String, value: String): Task[Boolean]
}

class CacheServiceImpl @Inject() (
  memCachedService: MemCachedService
) extends CacheService {
  private val log = LoggerFactory.getLogger(this.getClass.getName)

  private val cache = Map[String,String]()

  def get(key: String): Option[String] = {
    Option(memCachedService.client) match {
      case Some(memcachedClient) =>
        Option(memcachedClient.get(key)).map(_.toString)
      case None =>
        cache.get(key)
    }
  }

  private def toTask(operationFuture: OperationFuture[java.lang.Boolean]): Task[Boolean] = {
    val promise = scala.concurrent.Promise[Boolean]()

    operationFuture.addListener(new OperationCompletionListener {
      def onComplete(f: OperationFuture[_]): Unit = {
        val status = f.getStatus

        if(status.isSuccess) {
          log.debug(s"toTask[COMPLETE_SUCCESS][${operationFuture.get()}]")
          promise.success(operationFuture.get())
        } else {
          log.error(s"toTask[COMPLETE_FAIL][${status.getMessage}]")
          promise.failure(new Exception(status.getMessage))
        }
      }
    })
    ZIO.fromFuture { implicit ec =>
      log.debug(s"doTask[hack_to_suppress_unused_error][$ec]")
      promise.future
    }
  }

  def set(key: String, value: String): Task[Boolean] = {
    Option(memCachedService.client) match {
      case Some(memCachedClient) =>
        toTask(memCachedClient.set(key, 0, value))
      case None =>
        cache.addOne((key -> value))
        Task.succeed(true)
    }
  }
}

class CacheServiceModule extends AbstractModule {
  override def configure() = {
    bind(classOf[CacheService]).to(classOf[CacheServiceImpl])
  }
}
