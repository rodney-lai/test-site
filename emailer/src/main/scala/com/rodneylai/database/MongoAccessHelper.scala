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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.models.mongodb._

@Singleton
class MongoAccessHelper @Inject() (mongoHelper:MongoHelper,messageLogDao:MessageLogDao,resetPasswordDao:ResetPasswordDao) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  m_log.debug("init")

  def insertToMessageLog(emailUuid:java.util.UUID,email:String,emailType:String,toEmailAddress:String,now:java.util.Date):Future[Option[java.util.UUID]] = {
    if (mongoHelper.isActive) {
      for {
        collection <- messageLogDao.collectionFuture
        insertResult <- collection.insertOne(messageLogDao.toBson(
          MessageLog(
            emailUuid,
            "email",
            emailType,
            toEmailAddress,
            now
          )
        )).toFuture()
      } yield {
        Some(emailUuid)
      }
    } else {
      Future.successful(Some(emailUuid))
    }
  }

  def updateResetPassword(codeUuid:java.util.UUID,emailUuid:java.util.UUID,now:java.util.Date):Future[Option[java.util.UUID]] = {
    if (mongoHelper.isActive) {
      for {
        collection <- resetPasswordDao.collectionFuture
        updateMessageUuidResult <- collection.updateOne(
          and(
            equal("CodeUuid",MongoHelper.toStandardBinaryUUID(codeUuid)),
            exists("MessageUuid",false)
          ),
          Document(
            "$set" -> Document(
              "MessageUuid" -> MongoHelper.toStandardBinaryUUID(emailUuid),
              "UpdateDate" -> now
            )
          )
        ).toFuture()
        updateStatusResult <- collection.updateOne(
          and(
            equal("CodeUuid",MongoHelper.toStandardBinaryUUID(codeUuid)),
            equal("Status","queued")
          ),
          Document(
            "$set" -> Document(
              "Status" -> "sent",
              "UpdateDate" -> now
            )
          )
        ).toFuture()
      } yield {
        Some(emailUuid)
      }
    } else {
      Future.successful(Some(emailUuid))
    }
  }
}

class MongoAccessHelperModule extends AbstractModule {
  override def configure() = {
    bind(classOf[MongoAccessHelper]).asEagerSingleton
  }
}
