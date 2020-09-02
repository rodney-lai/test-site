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

package com.rodneylai.models.mongodb

import scala.concurrent.Future
import scala.util.{Failure,Success,Try}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.bson.types.{ObjectId}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonBinary,BsonDateTime,BsonNumber,BsonObjectId,BsonString}
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.database._

case class TrackingAction (
  actionUuid: java.util.UUID,
  actionType: String,
  referenceType: String,
  referenceId: Long,
  rawUrl: String,
  createDate: java.util.Date,
  id: Option[ObjectId] = None
)

@Singleton
class TrackingActionDao @Inject() (mongoHelper:MongoHelper) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  lazy val collectionFuture:Future[MongoCollection[Document]] = {
    Future.successful(mongoHelper.getCollection("TrackingAction"))
  }

  def fromBson(trackingActionBson:Document):Option[TrackingAction] = {
    Try(TrackingAction(
      MongoHelper.fromStandardBinaryUUID(trackingActionBson.get[BsonBinary]("ActionUuid").get.getData),
      trackingActionBson.get[BsonString]("ActionType").get.getValue,
      trackingActionBson.get[BsonString]("ReferenceType").get.getValue,
      trackingActionBson.get[BsonNumber]("ReferenceId").get.longValue,
      trackingActionBson.get[BsonString]("RawUrl").get.getValue,
      new java.util.Date(trackingActionBson.get[BsonDateTime]("CreateDate").get.getValue),
      Some(trackingActionBson.get[BsonObjectId]("_id").get.getValue)
    )) match {
      case Success(trackingAction) => Some(trackingAction)
      case Failure(ex) => {
        m_log.error(s"failed to convert bson to case class [$trackingActionBson]",ex)
        None
      }
    }
  }

  def toBson(trackingAction:TrackingAction):Document = {
    Document(
      "ActionUuid" -> MongoHelper.toStandardBinaryUUID(trackingAction.actionUuid),
      "ActionType" -> trackingAction.actionType,
      "ReferenceType" -> trackingAction.referenceType,
      "ReferenceId" -> trackingAction.referenceId,
      "RawUrl" -> trackingAction.rawUrl,
      "CreateDate" -> trackingAction.createDate
    ) ++
    trackingAction.id.map(objectId => Document("_id" -> BsonObjectId(objectId))).getOrElse(Nil)
  }
}

class TrackingActionDaoModule extends AbstractModule {
  override def configure() = {
    bind(classOf[TrackingActionDao]).asEagerSingleton
  }
}
