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

case class TrackingAction (
  id: Option[ObjectId],
  actionUuid: java.util.UUID,
  actionType: String,
  referenceType: String,
  referenceId: Long,
  rawUrl: String,
  createDateTimeUTC: java.util.Date
)

object TrackingAction {
  def getCollection:MongoCollection = {
    MongoHelper.getOrCreateCollection("TrackingAction")
  }
}

object TrackingActionMap {  
  def fromBson(trackingAction: DBObject):TrackingAction = {
    TrackingAction(
      Some(trackingAction.as[ObjectId]("_id")),
      MongoHelper.fromStandardBinaryUUID(trackingAction.as[Binary]("ActionUuid")),
      trackingAction.as[String]("ActionType"),
      trackingAction.as[String]("ReferenceType"),
      trackingAction.as[Long]("ReferenceId"),
      trackingAction.as[String]("RawUrl"),
      trackingAction.as[java.util.Date]("CreateDateTimeUTC")
    )
  }

  def toBson(trackingAction: TrackingAction): DBObject = {
    trackingAction.id match {
      case Some(objectId) => {
        MongoDBObject(
          "_id" -> objectId,
          "ActionUuid" -> MongoHelper.toStandardBinaryUUID(trackingAction.actionUuid),
          "ActionType" -> trackingAction.actionType,
          "ReferenceType" -> trackingAction.referenceType,
          "ReferenceId" -> trackingAction.referenceId,
          "RawUrl" -> trackingAction.rawUrl,
          "CreateDateTimeUTC" -> trackingAction.createDateTimeUTC
        )
      }
      case None => {
        MongoDBObject(
          "ActionUuid" -> MongoHelper.toStandardBinaryUUID(trackingAction.actionUuid),
          "ActionType" -> trackingAction.actionType,
          "ReferenceType" -> trackingAction.referenceType,
          "ReferenceId" -> trackingAction.referenceId,
          "RawUrl" -> trackingAction.rawUrl,
          "CreateDateTimeUTC" -> trackingAction.createDateTimeUTC
        )
      }
    }
  }
}

