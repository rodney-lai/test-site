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

import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure,Success,Try}
import java.util.{Calendar,Date}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.bson.types.{ObjectId}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonArray,BsonBinary,BsonDateTime,BsonObjectId,BsonString}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{IndexModel,IndexOptions}
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.auth.util._
import com.rodneylai.database._
import com.rodneylai.util._

case class UserAccount (
  userUuid: java.util.UUID,
  passwordHash: String,
  emailAddress: String,
  emailAddressLowerCase: String,
  name: String,
  friendlyUrl: String,
  roleList: Set[String],
  status: String,
  updateDate: java.util.Date,
  createDate: java.util.Date,
  id: Option[ObjectId] = None
) {
  def isAdmin:Boolean = roleList.contains("admin")
}

@Singleton
class UserAccountDao @Inject() (testAccountHelper:TestAccountHelper,mongoHelper:MongoHelper) {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)
  private lazy val m_testAccountNow:java.util.Date = Calendar.getInstance.getTime
  private lazy val m_testNormalUserUuid:java.util.UUID = java.util.UUID.randomUUID
  private lazy val m_testAdminUserUuid:java.util.UUID = java.util.UUID.randomUUID
  private lazy val m_testDeveloperUserUuid:java.util.UUID = java.util.UUID.randomUUID
  private lazy val m_testNormalAccount:UserAccount = UserAccount( m_testNormalUserUuid,
                                                                  testAccountHelper.testPasswordHash,
                                                                  "normal_user@rodneylai.com",
                                                                  "normal_user@rodneylai.com",
                                                                  "Test User",
                                                                  "test-user",
                                                                  Set[String](),
                                                                  "active",
                                                                  m_testAccountNow,
                                                                  m_testAccountNow
                                                      )
  private lazy val m_testAdminAccount:UserAccount = UserAccount(m_testAdminUserUuid,
                                                                testAccountHelper.testPasswordHash,
                                                                "admin_user@rodneylai.com",
                                                                "admin_user@rodneylai.com",
                                                                "Test Admin",
                                                                "test-admin",
                                                                Set[String]("admin"),
                                                                "active",
                                                                m_testAccountNow,
                                                                m_testAccountNow
                                                  )
  private lazy val m_testDeveloperAccount:UserAccount = UserAccount(m_testDeveloperUserUuid,
                                                                    testAccountHelper.testPasswordHash,
                                                                    "developer_user@rodneylai.com",
                                                                    "developer_user@rodneylai.com",
                                                                    "Test Developer",
                                                                    "test-developer",
                                                                    Set[String]("admin","developer"),
                                                                    "active",
                                                                    m_testAccountNow,
                                                                    m_testAccountNow
                                                        )
  private lazy val m_testAccountsByEmailAddress:Map[String,UserAccount] = Map("normal_user@rodneylai.com" -> m_testNormalAccount,
                                                                              "admin_user@rodneylai.com" -> m_testAdminAccount,
                                                                              "developer_user@rodneylai.com" -> m_testDeveloperAccount
                                                                          )
  private lazy val m_testAccountsByFriendlyUrl:Map[String,UserAccount] = Map( "test-user" -> m_testNormalAccount,
                                                                              "test-admin" -> m_testAdminAccount,
                                                                              "test-developer" -> m_testDeveloperAccount
                                                                          )
  private lazy val m_testAccountsByUserUuid:Map[java.util.UUID,UserAccount] = Map(m_testNormalUserUuid -> m_testNormalAccount,
                                                                                  m_testAdminUserUuid -> m_testAdminAccount,
                                                                                  m_testDeveloperUserUuid -> m_testDeveloperAccount
                                                                              )

  def getTestAccounts:Option[Seq[UserAccount]] = {
    if (mongoHelper.isActive) {
      None  // do not use test accounts if mongo is active
    } else {
      Some(Seq[UserAccount](m_testAdminAccount,m_testDeveloperAccount,m_testNormalAccount))
    }
  }

  lazy val collectionFuture:Future[MongoCollection[Document]] = {
    val collection:MongoCollection[Document] = mongoHelper.getCollection("UserAccount")

    for {
      createIndexesResult <- collection.createIndexes(
        Seq(
          IndexModel(Document("UserUuid" -> 1), (new IndexOptions).name("UserAccount_UserUuid").unique(true)),
          IndexModel(Document("EmailAddressLowerCase" -> 1), (new IndexOptions).name("UserAccount_EmailAddressLowerCase").unique(true)),
          IndexModel(Document("FriendlyUrl" -> 1), (new IndexOptions).name("UserAccount_FriendlyUrl").unique(true))
        )
      ).toFuture
    } yield {
      m_log.trace(s"createIndexes[$createIndexesResult]")
      collection
    }
  }

  def findByUserUuid(userUuid:java.util.UUID):Future[Option[UserAccount]] = {
    if (mongoHelper.isActive) {
      for {
        collection <- collectionFuture
        results <- collection.find(equal("UserUuid",MongoHelper.toStandardBinaryUUID(userUuid))).toFuture
      } yield {
        if (results.isEmpty) {
          None
        } else if (results.size == 1) {
          fromBson(results.head)
        } else {
          throw new Exception("more than one user with user uuid = " + userUuid.toString)
        }
      }
    } else {
      Future.successful(m_testAccountsByUserUuid.get(userUuid))
    }
  }

  def findByEmailAddress(emailAddress:String):Future[Option[UserAccount]] = {
    if (mongoHelper.isActive) {
      for {
        collection <- collectionFuture
        results <- collection.find(equal("EmailAddressLowerCase",emailAddress.toLowerCase)).toFuture
      } yield {
        if (results.isEmpty) {
          None
        } else if (results.size == 1) {
          fromBson(results.head)
        } else {
          throw new Exception("more than one user with email address = " + emailAddress)
        }
      }
    } else {
      Future.successful(m_testAccountsByEmailAddress.get(emailAddress))
    }
  }

  def findByFriendlyUrl(friendlyUrl:String):Future[Option[UserAccount]] = {
    if (mongoHelper.isActive) {
      for {
        collection <- collectionFuture
        results <- collection.find(equal("FriendlyUrl",friendlyUrl.toLowerCase)).toFuture
      } yield {
        if (results.isEmpty) {
          None
        } else if (results.size == 1) {
          fromBson(results.head)
        } else {
          throw new Exception("more than one user with friendly url = " + friendlyUrl)
        }
      }
    } else {
      Future.successful(m_testAccountsByFriendlyUrl.get(friendlyUrl))
    }
  }

  def fromBson(userAccountBson:Document):Option[UserAccount] = {
    Try(UserAccount(
      MongoHelper.fromStandardBinaryUUID(userAccountBson.get[BsonBinary]("UserUuid").get.getData),
      userAccountBson.get[BsonString]("PasswordHash").get.getValue,
      userAccountBson.get[BsonString]("EmailAddress").get.getValue,
      userAccountBson.get[BsonString]("EmailAddressLowerCase").get.getValue,
      userAccountBson.get[BsonString]("Name").get.getValue,
      userAccountBson.get[BsonString]("FriendlyUrl").get.getValue,
      JavaConversions.asScalaBuffer(userAccountBson.get[BsonArray]("RoleList").get.getValues).map(_ match { case bsonString:BsonString => bsonString.getValue }).toSet,
      userAccountBson.get[BsonString]("Status").get.getValue,
      new java.util.Date(userAccountBson.get[BsonDateTime]("UpdateDate").get.getValue),
      new java.util.Date(userAccountBson.get[BsonDateTime]("CreateDate").get.getValue),
      Some(userAccountBson.get[BsonObjectId]("_id").get.getValue)
    )) match {
      case Success(userAccount) => Some(userAccount)
      case Failure(ex) => {
        m_log.error(s"failed to convert bson to case class [$userAccountBson]",ex)
        None
      }
    }
  }

  def toBson(userAccount: UserAccount): Document = {
    Document(
      "UserUuid" -> MongoHelper.toStandardBinaryUUID(userAccount.userUuid),
      "PasswordHash" -> userAccount.passwordHash,
      "EmailAddress" -> userAccount.emailAddress,
      "EmailAddressLowerCase" -> userAccount.emailAddressLowerCase,
      "Name" -> userAccount.name,
      "FriendlyUrl" -> userAccount.friendlyUrl,
      "RoleList" -> BsonArray(userAccount.roleList.map(BsonString(_)).toSeq),
      "Status" -> userAccount.status,
      "UpdateDate" -> userAccount.updateDate,
      "CreateDate" -> userAccount.createDate
    ) ++
    userAccount.id.map(objectId => Document("_id" -> BsonObjectId(objectId))).getOrElse(Nil)
  }
}

class UserAccountDaoModule extends AbstractModule {
  def configure() = {
    bind(classOf[UserAccountDao]).asEagerSingleton
  }
}
