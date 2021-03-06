/**
 *
 * Copyright (c) 2015-2020 Rodney S.K. Lai
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

package com.rodneylai.util

import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.slf4j.{Logger,LoggerFactory}
import scala.concurrent.duration.Duration

@Singleton
class CacheHelper @Inject() (
  cache:play.api.cache.DefaultSyncCacheApi,
) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  m_log.debug("init")

  def get[T](key: String): Option[T] = cache.get(key)
  def set(key: String, value: Any, expiration: Duration = Duration.Inf): Unit = {
    cache.set(key, value, expiration)
  }
}

class CacheHelperModule extends AbstractModule {
  override def configure() = {
    bind(classOf[ConfigHelper]).asEagerSingleton
  }
}
