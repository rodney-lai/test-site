/**
 *
 * Copyright (c) 2015 Rodney S.K. Lai
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

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.DBObject
import org.bson.types._
import com.rodneylai.util._

case class TrackingEvent (
  id: Option[ObjectId],
  trackingUuid: java.util.UUID,
  sourceUuid: java.util.UUID,
  actionType: String,
  actionUuid: java.util.UUID,
  userUuid: java.util.UUID,
  ipAddress: String,
  sessionId: String,
  userAgent: String,
  urlReferrer: String,
  createDateTimeUTC: java.util.Date
)

object TrackingEvent {
  def getCollection:MongoCollection = {
    MongoHelper.getOrCreateCollection("TrackingEvent")
  }
}

object TrackingEventMap {  

  def fromBson(trackingEvent: DBObject):TrackingEvent = {
    TrackingEvent(
      Some(trackingEvent.as[ObjectId]("_id")),
      MongoHelper.fromStandardBinaryUUID(trackingEvent.as[Binary]("TrackingUuid")),
      MongoHelper.fromStandardBinaryUUID(trackingEvent.as[Binary]("SourceUuid")),
      trackingEvent.as[String]("ActionType"),
      MongoHelper.fromStandardBinaryUUID(trackingEvent.as[Binary]("ActionUuid")),
      MongoHelper.fromStandardBinaryUUID(trackingEvent.as[Binary]("UserUuid")),
      trackingEvent.as[String]("IpAddress"),
      trackingEvent.as[String]("SessionId"),
      trackingEvent.as[String]("UserAgent"),
      trackingEvent.as[String]("UrlReferrer"),
      trackingEvent.as[java.util.Date]("CreateDateTimeUTC")
    )
  }

  def toBson(trackingEvent: TrackingEvent): DBObject = {
    trackingEvent.id match {
      case Some(objectId) => {
        MongoDBObject(
          "_id" -> objectId,
          "TrackingUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.trackingUuid),
          "SourceUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.sourceUuid),
          "ActionType" -> trackingEvent.actionType,
          "ActionUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.actionUuid),
          "UserUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.userUuid),
          "IpAddress" -> trackingEvent.ipAddress,
          "SessionId" -> trackingEvent.sessionId,
          "UserAgent" -> trackingEvent.userAgent,
          "UrlReferrer" -> trackingEvent.urlReferrer,
          "CreateDateTimeUTC" -> trackingEvent.createDateTimeUTC
        )
      }
      case None => {
        MongoDBObject(
          "TrackingUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.trackingUuid),
          "SourceUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.sourceUuid),
          "ActionType" -> trackingEvent.actionType,
          "ActionUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.actionUuid),
          "UserUuid" -> MongoHelper.toStandardBinaryUUID(trackingEvent.userUuid),
          "IpAddress" -> trackingEvent.ipAddress,
          "SessionId" -> trackingEvent.sessionId,
          "UserAgent" -> trackingEvent.userAgent,
          "UrlReferrer" -> trackingEvent.urlReferrer,
          "CreateDateTimeUTC" -> trackingEvent.createDateTimeUTC
        )
      }
    }
  }
}

