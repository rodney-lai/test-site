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

package com.rodneylai.auth

import play.api.{Application,Configuration,Environment}
import play.api.mvc._
import scala.concurrent.{ExecutionContext,Future}
import java.util.{Calendar,Date}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.mindrot.jbcrypt._
import org.mongodb.scala._
import com.rodneylai.auth.util._
import com.rodneylai.models.mongodb._
import com.rodneylai.database._
import com.rodneylai.util._

@Singleton
class AuthHelper @Inject() (override val environment:Environment,override val configuration:Configuration,mongoHelper:MongoHelper,testAccountHelper:TestAccountHelper,userAccountDao:UserAccountDao,override val accountDao:AccountDao) extends AuthConfigImpl
{

  def hashPassword(password:String):String = {
    BCrypt.hashpw(password, BCrypt.gensalt(14))
  }

  def validatePassword(password:String,passwordHash:String):Boolean = BCrypt.checkpw(password, passwordHash)

  def initTestUsers(implicit ctx: ExecutionContext):Future[Option[Seq[UserAccount]]] = {
    if (mongoHelper.isActive) {
      for {
        collection <- userAccountDao.collectionFuture
        userAccounts <- Future.sequence(
          testAccountHelper.testAccounts.map(account => {
            for {
              userAccountOption <- userAccountDao.findByEmailAddress(account.email)
              userAccount <- userAccountOption match {
                case Some(userAccount) => Future.successful(userAccount)
                case None => {
                  val now:java.util.Date = Calendar.getInstance.getTime
                  val userAccount:UserAccount = UserAccount(java.util.UUID.randomUUID,
                                                            testAccountHelper.testPasswordHash,
                                                            account.email,
                                                            account.email,
                                                            account.name,
                                                            account.friendlyUrl,
                                                            account.roleList,
                                                            "active",
                                                            now,
                                                            now)

                  for {
                    _ <- collection.insertOne(userAccountDao.toBson(userAccount)).toFuture
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
        case Some(id) => accountDao.findById(id)
        case None => Future.successful(None)
      }
    } yield userOption
  }

}

class AuthHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[AuthHelper]).asEagerSingleton
  }
}
