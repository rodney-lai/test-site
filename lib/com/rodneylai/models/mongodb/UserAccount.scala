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

import java.util.{Calendar,Date}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.DBObject
import org.bson.types._
import com.rodneylai.auth._
import com.rodneylai.util._

case class UserAccount (
  id: Option[ObjectId],
  userUuid: java.util.UUID,
  passwordHash: String,
  emailAddress: String,
  emailAddressLowerCase: String,
  name: String,
  friendlyUrl: String,
  roleList: Set[String],
  status: String,
  updateDateTimeUTC: java.util.Date,
  createDateTimeUTC: java.util.Date
) {
  def isAdmin:Boolean = roleList.contains("admin")
}

object UserAccount {
  private lazy val m_testAccountNow:java.util.Date = Calendar.getInstance.getTime
  private lazy val m_testNormalUserUuid:java.util.UUID = java.util.UUID.randomUUID
  private lazy val m_testAdminUserUuid:java.util.UUID = java.util.UUID.randomUUID
  private lazy val m_testDeveloperUserUuid:java.util.UUID = java.util.UUID.randomUUID
  private lazy val m_testNormalAccount:UserAccount = UserAccount( None,
                                                                  m_testNormalUserUuid,
                                                                  AuthHelper.testPasswordHash,
                                                                  "normal_user@rodneylai.com",
                                                                  "normal_user@rodneylai.com",
                                                                  "Test User",
                                                                  "test-user",
                                                                  Set[String](),
                                                                  "active",
                                                                  m_testAccountNow,
                                                                  m_testAccountNow
                                                      )
  private lazy val m_testAdminAccount:UserAccount = UserAccount(None,
                                                                m_testAdminUserUuid,
                                                                AuthHelper.testPasswordHash,
                                                                "admin_user@rodneylai.com",
                                                                "admin_user@rodneylai.com",
                                                                "Test Admin",
                                                                "test-admin",
                                                                Set[String]("admin"),
                                                                "active",
                                                                m_testAccountNow,
                                                                m_testAccountNow
                                                  )
  private lazy val m_testDeveloperAccount:UserAccount = UserAccount(None,
                                                                    m_testDeveloperUserUuid,
                                                                    AuthHelper.testPasswordHash,
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
    if (MongoHelper.isActive) {
      None  // do not use test accounts if mongo is active
    } else {
      Some(Seq[UserAccount](m_testAdminAccount,m_testDeveloperAccount,m_testNormalAccount))
    }
  }

  def getCollection:MongoCollection = {
    val collection = MongoHelper.getOrCreateCollection("UserAccount")

    collection.createIndex( MongoDBObject("UserUuid" -> 1), MongoDBObject("unique" -> true, "name" -> "UserAccount_UserUuid") )
    collection.createIndex( MongoDBObject("EmailAddressLowerCase" -> 1), MongoDBObject("unique" -> true, "name" -> "UserAccount_EmailAddressLowerCase") )
    collection.createIndex( MongoDBObject("FriendlyUrl" -> 1), MongoDBObject("unique" -> true, "name" -> "UserAccount_FriendlyUrl") )
    collection
  }

  def findByUserUuid(userUuid:java.util.UUID): Option[UserAccount] = {
    if (MongoHelper.isActive) {
      UserAccount.getCollection.findOne( MongoDBObject("UserUuid" -> MongoHelper.toStandardBinaryUUID(userUuid)) ) match {
        case Some(userAccountBson) => Some(UserAccountMap.fromBson(userAccountBson))
        case None => None
      }
    } else {
      m_testAccountsByUserUuid.get(userUuid)
    }
  }

  def findByEmailAddress(emailAddress:String): Option[UserAccount] = {
    if (MongoHelper.isActive) {
      UserAccount.getCollection.findOne( MongoDBObject("EmailAddressLowerCase" -> emailAddress.toLowerCase) ) match {
        case Some(userAccountBson) => Some(UserAccountMap.fromBson(userAccountBson))
        case None => None
      }
    } else {
      m_testAccountsByEmailAddress.get(emailAddress)
    }
  }

  def findByFriendlyUrl(friendlyUrl:String): Option[UserAccount] = {
    if (MongoHelper.isActive) {
      UserAccount.getCollection.findOne( MongoDBObject("FriendlyUrl" -> friendlyUrl.toLowerCase) ) match {
        case Some(userAccountBson) => Some(UserAccountMap.fromBson(userAccountBson))
        case None => None
      }
    } else {
      m_testAccountsByFriendlyUrl.get(friendlyUrl)
    }
  }

}

object UserAccountMap {  
  def fromBson(userAccount: DBObject):UserAccount = {
    UserAccount(
      Some(userAccount.as[ObjectId]("_id")),
      MongoHelper.fromStandardBinaryUUID(userAccount.as[Binary]("UserUuid")),
      userAccount.as[String]("PasswordHash"),
      userAccount.as[String]("EmailAddress"),
      userAccount.as[String]("EmailAddressLowerCase"),
      userAccount.as[String]("Name"),
      userAccount.as[String]("FriendlyUrl"),
      userAccount.getAsOrElse[Seq[String]]("RoleList",Seq[String]()).toSet,
      userAccount.as[String]("Status"),
      userAccount.as[java.util.Date]("UpdateDateTimeUTC"),
      userAccount.as[java.util.Date]("CreateDateTimeUTC")
    )
  }

  def toBson(userAccount: UserAccount): DBObject = {
    userAccount.id match {
      case Some(objectId) => {
        MongoDBObject(
          "_id" -> objectId,
          "UserUuid" -> MongoHelper.toStandardBinaryUUID(userAccount.userUuid),
          "PasswordHash" -> userAccount.passwordHash,
          "EmailAddress" -> userAccount.emailAddress,
          "EmailAddressLowerCase" -> userAccount.emailAddressLowerCase,
          "Name" -> userAccount.name,
          "FriendlyUrl" -> userAccount.friendlyUrl,
          "RoleList" -> MongoDBList(userAccount.roleList.toSeq:_*),
          "Status" -> userAccount.status,
          "UpdateDateTimeUTC" -> userAccount.updateDateTimeUTC,
          "CreateDateTimeUTC" -> userAccount.createDateTimeUTC
        )
      }
      case None => {
        MongoDBObject(
          "UserUuid" -> MongoHelper.toStandardBinaryUUID(userAccount.userUuid),
          "PasswordHash" -> userAccount.passwordHash,
          "EmailAddress" -> userAccount.emailAddress,
          "EmailAddressLowerCase" -> userAccount.emailAddressLowerCase,
          "Name" -> userAccount.name,
          "FriendlyUrl" -> userAccount.friendlyUrl,
          "RoleList" -> MongoDBList(userAccount.roleList.toSeq:_*),
          "Status" -> userAccount.status,
          "UpdateDateTimeUTC" -> userAccount.updateDateTimeUTC,
          "CreateDateTimeUTC" -> userAccount.createDateTimeUTC
        )
      }
    }
  }
}


