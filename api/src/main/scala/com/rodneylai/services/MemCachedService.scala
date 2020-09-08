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

import com.google.inject.AbstractModule
import net.spy.memcached.AddrUtil
import net.spy.memcached.ConnectionFactoryBuilder
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import net.spy.memcached.MemcachedClient
import net.spy.memcached.auth.AuthDescriptor
import net.spy.memcached.auth.PlainCallbackHandler
import org.slf4j.LoggerFactory
import pureconfig._
import pureconfig.generic.auto._

trait MemCachedService {
  val client: MemcachedClient
}

class MemCachedServiceImpl extends MemCachedService {
  private val log = LoggerFactory.getLogger(this.getClass.getName)

  case class MemCachedConfig(
    username: Option[String],
    password: Option[String],
    addresses: String
  )

  val client = ConfigSource.default.at("memcached").load[MemCachedConfig] match {
    case Right(config) =>
      val builder = new ConnectionFactoryBuilder()

      builder.setProtocol( Protocol.BINARY )

      (config.username, config.password) match {
        case (Some(username),Some(password)) =>
          val authType = Array("PLAIN")
          val authDescriptor = new AuthDescriptor( authType, new PlainCallbackHandler( username, password ) )
          builder.setAuthDescriptor( authDescriptor )
        case _ =>
      }

      new MemcachedClient( builder.build(), AddrUtil.getAddresses( config.addresses ) )
    case Left(ex) =>
      log.error("[failed_to_load_config]",ex)
      null
  }
}

class MemCachedServiceModule extends AbstractModule {
  override def configure() = {
    bind(classOf[MemCachedService]).to(classOf[MemCachedServiceImpl])
  }
}
