/**
 *
 * Copyright (c) 2015-2016 Rodney S.K. Lai
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
import scala.concurrent.{ExecutionContext,Future}
import org.mindrot.jbcrypt._
import org.mongodb.scala._
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

  def initTestUsers(implicit ctx: ExecutionContext):Future[Option[Seq[UserAccount]]] = {
    if (MongoHelper.isActive) {
      for {
        collection <- UserAccountDao.collectionFuture
        userAccounts <- Future.sequence(
          m_testAccounts.map(account => {
            for {
              userAccountOption <- UserAccountDao.findByEmailAddress(account.email)
              userAccount <- userAccountOption match {
                case Some(userAccount) => Future.successful(userAccount)
                case None => {
                  val now:java.util.Date = Calendar.getInstance.getTime
                  val userAccount:UserAccount = UserAccount(java.util.UUID.randomUUID,
                                                            testPasswordHash,
                                                            account.email,
                                                            account.email,
                                                            account.name,
                                                            account.friendlyUrl,
                                                            account.roleList,
                                                            "active",
                                                            now,
                                                            now)

                  for {
                    _ <- collection.insertOne(UserAccountDao.toBson(userAccount)).toFuture
                  } yield userAccount
                }
              }
            } yield userAccount
          })
        )
      } yield Some(userAccounts)
    } else {
      Future.successful(None)
    }
  }

  def getCurrentUserId(request: RequestHeader)(implicit ctx: ExecutionContext):Future[Option[Id]] = {
    tokenAccessor.extract(request) match {
      case Some(token) => idContainer.get(token)
      case None => Future.successful(None)
    }
  }

  def getCurrentUser(request: RequestHeader)(implicit ctx: ExecutionContext):Future[Option[User]] = {
    for {
      idOption <- getCurrentUserId(request)
      userOption <- idOption match {
        case Some(id) => Account.findById(id)
        case None => Future.successful(None)
      }
    } yield userOption
  }

}
