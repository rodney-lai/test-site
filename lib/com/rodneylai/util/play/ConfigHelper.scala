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

package com.rodneylai.util

import play.api.{Configuration,Environment}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.slf4j.{Logger,LoggerFactory}

@Singleton
class ConfigHelper @Inject() (configuration:Configuration)
{
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def getString(name:String):Option[String] = configuration.getString(name)
  def getInt(name:String):Option[Int] = configuration.getInt(name)
}

class ConfigHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[ConfigHelper]).asEagerSingleton
  }
}
