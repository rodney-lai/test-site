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

package com.rodneylai.database

import play.api.mvc._
import scala.concurrent.Future
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule

@Singleton
class TrackingHelper @Inject() ()
{
  def getTrackingActionByTypeAndUrl(actionType:String,rawUrl:String):Future[Option[Unit]] = {
    Future.successful(None)
  }

  def trackEventByTypeAndUrl(requestHeader:RequestHeader,trackingUuid:java.util.UUID,userUuid:java.util.UUID,actionType:String,rawUrl:String):Future[Option[Long]] = {
    Future.successful(None)
  }
}

class TrackingHelperStubModule extends AbstractModule {
  override def configure() = {
    bind(classOf[TrackingHelper]).asEagerSingleton
  }
}
