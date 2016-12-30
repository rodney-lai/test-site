/**
 *
 * Copyright (c) 2015-2016 Rodney S.K. Lai
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

package com.rodneylai.models.mongodb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure,Success,Try}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.bson.types.{ObjectId}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonBinary,BsonDateTime,BsonObjectId,BsonString}
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.database._
import com.rodneylai.util._

case class TrackingEvent (
  trackingUuid: java.util.UUID,
  sourceUuid: java.util.UUID,
  actionType: String,
  actionUuid: java.util.UUID,
  userUuid: java.util.UUID,
  ipAddress: String,
  sessionId: String,
  userAgent: String,
  urlReferrer: String,
  createDate: java.util.Date,
  id: Option[ObjectId] = None
)

@Singleton
class TrackingEventDao @Inject() (mongoHelper:MongoHelper) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  lazy val collectionFuture:Future[MongoCollection[Document]] = {
    Future.successful(mongoHelper.getCollection("TrackingEvent"))
  }

  def fromBson(trackingEventBson:Document):Option[TrackingEvent] = {
    Try(TrackingEvent(
      MongoHelper.fromStandardBinaryUUID(trackingEventBson.get[BsonBinary]("TrackingUuid").get.getData),
      MongoHelper.fromStandardBinaryUUID(trackingEventBson.get[BsonBinary]("SourceUuid").get.getData),
      trackingEventBson.get[BsonString]("ActionType").get.getValue,
      MongoHelper.fromStandardBinaryUUID(trackingEventBson.get[BsonBinary]("ActionUuid").get.getData),
      MongoHelper.fromStandardBinaryUUID(trackingEventBson.get[BsonBinary]("UserUuid").get.getData),
      trackingEventBson.get[BsonString]("IpAddress").get.getValue,
      trackingEventBson.get[BsonString]("SessionId").get.getValue,
      trackingEventBson.get[BsonString]("UserAgent").get.getValue,
      trackingEventBson.get[BsonString]("UrlReferrer").get.getValue,
      new java.util.Date(trackingEventBson.get[BsonDateTime]("CreateDate").get.getValue),
      Some(trackingEventBson.get[BsonObjectId]("_id").get.getValue)
    )) match {
      case Success(trackingEvent) => Some(trackingEvent)
      case Failure(ex) => {
        m_log.error(s"failed to convert bson to case class [$trackingEventBson]",ex)
        None
      }
    }
  }

  def toBson(trackingEvent:TrackingEvent):Document = {
    Document(
      "TrackingUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.trackingUuid),
      "SourceUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.sourceUuid),
      "ActionType" -> trackingEvent.actionType,
      "ActionUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.actionUuid),
      "UserUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.userUuid),
      "IpAddress" -> trackingEvent.ipAddress,
      "SessionId" -> trackingEvent.sessionId,
      "UserAgent" -> trackingEvent.userAgent,
      "UrlReferrer" -> trackingEvent.urlReferrer,
      "CreateDate" -> trackingEvent.createDate
    ) ++
    trackingEvent.id.map(objectId => Document("_id" -> BsonObjectId(objectId))).getOrElse(Nil)
  }
}

class TrackingEventDaoModule extends AbstractModule {
  def configure() = {
    bind(classOf[TrackingEventDao]).asEagerSingleton
  }
}
