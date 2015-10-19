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

package com.rodneylai.auth

import play.api.mvc._
import java.util.{Calendar,Date}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}
import org.mindrot.jbcrypt._
import com.mongodb.casbah.Imports._
import com.rodneylai.models.mongodb._
import com.rodneylai.util._

object AuthHelper extends AuthConfigImpl
{
  case class TestAccount(email:String,name:String,friendlyUrl:String,roleList:Set[String])

  private val m_testAccounts:Seq[TestAccount] = Seq(TestAccount("normal_user@rodneylai.com","Test User","test-user",Set[String]()),
                                                    TestAccount("admin_user@rodneylai.com","Test Admin","test-admin",Set[String]("admin")),
                                                    TestAccount("developer_user@rodneylai.com","Test Developer","test-developer",Set[String]("admin","developer"))
                                                )
  private lazy val m_testPassword:String =  play.api.Play.current.configuration.getString("test.password") match {
                                              case Some(testPassword) if (testPassword != "changeme") => testPassword
                                              case Some(testPassword) if (testPassword == "changeme") => throw new Exception("Change default value of test.password config key.")
                                              case None => throw new Exception("Missing test.password config key.")
                                            }

  def testPasswordHash:String = BCrypt.hashpw(m_testPassword, BCrypt.gensalt(14))

  def hashPassword(password:String):String = {
    BCrypt.hashpw(password, BCrypt.gensalt(14))
  }

  def validatePassword(password:String,passwordHash:String):Boolean = BCrypt.checkpw(password, passwordHash)

  def initTestUsers:Option[Seq[UserAccount]] = {
    if (MongoHelper.isActive) {
      val userAccountCollection:MongoCollection = UserAccount.getCollection

      Some(m_testAccounts.map(account => {
        userAccountCollection.findOne( MongoDBObject("EmailAddressLowerCase" -> account.email) ) match {
          case Some(userAccount) => UserAccountMap.fromBson(userAccount)
          case None => {
            val now:java.util.Date = Calendar.getInstance.getTime
            val userAccount:UserAccount = UserAccount(None,
                                                      java.util.UUID.randomUUID,
                                                      testPasswordHash,
                                                      account.email,
                                                      account.email,
                                                      account.name,
                                                      account.friendlyUrl,
                                                      account.roleList,
                                                      "active",
                                                      now,
                                                      now)

            userAccountCollection.insert(UserAccountMap.toBson(userAccount))
            userAccount
          }
        }
      }))
    } else {
      None
    }
  }

  def getCurrentUserId(implicit request: RequestHeader): Future[Option[Id]] = {
    tokenAccessor.extract(request) match {
      case Some(token) => idContainer.get(token)
      case None => Future(None)
    }
  }

  def getCurrentUser(implicit request: RequestHeader): Future[Option[User]] = {
    getCurrentUserId(request) map {
      case Some(id) => Account.findByIdNow(id)
      case None => None
    }
  }

}




