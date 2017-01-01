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

case class ResetPassword (
  codeUuid: java.util.UUID,
  userUuid: java.util.UUID,
  status: String,
  messageUuid: Option[java.util.UUID],
  updateDate: java.util.Date,
  createDate: java.util.Date,
  id: Option[ObjectId] = None
)

@Singleton
class ResetPasswordDao @Inject() (mongoHelper:MongoHelper) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  lazy val collectionFuture:Future[MongoCollection[Document]] = {
    val collection:MongoCollection[Document] = mongoHelper.getCollection("ResetPassword")

    for {
      createIndexesResult <- collection.createIndexes(
        Seq(
          IndexModel(Document("CodeUuid" -> 1), (new IndexOptions).name("ResetPassword_CodeUuid").unique(true))
        )
      ).toFuture
    } yield {
      m_log.trace(s"createIndexes[$createIndexesResult]")
      collection
    }
  }

  def fromBson(resetPasswordBson:Document):Option[ResetPassword] = {
    Try(ResetPassword(
      MongoHelper.fromStandardBinaryUUID(resetPasswordBson.get[BsonBinary]("CodeUuid").get.getData),
      MongoHelper.fromStandardBinaryUUID(resetPasswordBson.get[BsonBinary]("UserUuid").get.getData),
      resetPasswordBson.get[BsonString]("Status").get.getValue,
      resetPasswordBson.get[BsonBinary]("MessageUuid").map(bson => MongoHelper.fromStandardBinaryUUID(bson.getData)),
      new java.util.Date(resetPasswordBson.get[BsonDateTime]("UpdateDate").get.getValue),
      new java.util.Date(resetPasswordBson.get[BsonDateTime]("CreateDate").get.getValue),
      Some(resetPasswordBson.get[BsonObjectId]("_id").get.getValue)
    )) match {
      case Success(resetPassword) => Some(resetPassword)
      case Failure(ex) => {
        m_log.error(s"failed to convert bson to case class [$resetPasswordBson]",ex)
        None
      }
    }
  }

  def toBson(resetPassword:ResetPassword):Document = {
    Document(
      "CodeUuid" -> MongoHelper.toStandardBinaryUUID(resetPassword.codeUuid),
      "UserUuid" -> MongoHelper.toStandardBinaryUUID(resetPassword.userUuid),
      "Status" -> resetPassword.status,
      "UpdateDate" -> resetPassword.updateDate,
      "CreateDate" -> resetPassword.createDate
    ) ++
    resetPassword.messageUuid.map(uuid => Document("MessageUuid" -> MongoHelper.toStandardBinaryUUID(uuid))).getOrElse(Nil) ++
    resetPassword.id.map(objectId => Document("_id" -> BsonObjectId(objectId))).getOrElse(Nil)
  }
}

class ResetPasswordDaoModule extends AbstractModule {
  def configure() = {
    bind(classOf[ResetPasswordDao]).asEagerSingleton
  }
}
