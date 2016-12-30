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
import org.mongodb.scala.model.{IndexModel,IndexOptions}
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.database._
import com.rodneylai.util._

case class MessageLog (
  messageUuid: java.util.UUID,
  deliveryMethod: String,
  messageType: String,
  messageAddress: String,
  createDate: java.util.Date,
  id: Option[ObjectId] = None
)

@Singleton
class MessageLogDao @Inject() (mongoHelper:MongoHelper) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  lazy val collectionFuture:Future[MongoCollection[Document]] = {
    val collection:MongoCollection[Document] = mongoHelper.getCollection("MessageLog")

    for {
      createIndexesResult <- collection.createIndexes(
        Seq(
          IndexModel(Document("MessageUuid" -> 1), (new IndexOptions).name("MessageLog_MessageUuid").unique(true))
        )
      ).toFuture
    } yield {
      m_log.trace(s"createIndexes[$createIndexesResult]")
      collection
    }
  }

  def fromBson(messageLogBson:Document):Option[MessageLog] = {
    Try(MessageLog(
      MongoHelper.fromStandardBinaryUUID(messageLogBson.get[BsonBinary]("MessageUuid").get.getData),
      messageLogBson.get[BsonString]("DeliveryMethod").get.getValue,
      messageLogBson.get[BsonString]("MessageType").get.getValue,
      messageLogBson.get[BsonString]("MessageAddress").get.getValue,
      new java.util.Date(messageLogBson.get[BsonDateTime]("CreateDate").get.getValue),
      Some(messageLogBson.get[BsonObjectId]("_id").get.getValue)
    )) match {
      case Success(messageLog) => Some(messageLog)
      case Failure(ex) => {
        m_log.error(s"failed to convert bson to case class [$messageLogBson]",ex)
        None
      }
    }
  }

  def toBson(messageLog:MessageLog):Document = {
    Document(
      "MessageUuid" -> MongoHelper.toStandardBinaryUUID(messageLog.messageUuid),
      "DeliveryMethod" -> messageLog.deliveryMethod,
      "MessageType" -> messageLog.messageType,
      "MessageAddress" -> messageLog.messageAddress,
      "CreateDate" -> messageLog.createDate
    ) ++
    messageLog.id.map(objectId => Document("_id" -> BsonObjectId(objectId))).getOrElse(Nil)
  }
}

class MessageLogDaoModule extends AbstractModule {
  def configure() = {
    bind(classOf[MessageLogDao]).asEagerSingleton
  }
}
