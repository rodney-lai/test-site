package com.rodneylai.database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.database._
import com.rodneylai.models.mongodb._

@Singleton
class MongoAccessHelper @Inject() (mongoHelper:MongoHelper,messageLogDao:MessageLogDao,resetPasswordDao:ResetPasswordDao) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

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
        )).toFuture
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
        ).toFuture
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
        ).toFuture
      } yield {
        Some(emailUuid)
      }
    } else {
      Future.successful(Some(emailUuid))
    }
  }
}

class MongoAccessHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[MongoAccessHelper]).asEagerSingleton
  }
}
