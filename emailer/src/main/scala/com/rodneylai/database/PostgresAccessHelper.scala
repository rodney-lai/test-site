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
import slick.jdbc.PostgresProfile.backend.{Database}
import com.google.inject.Guice
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.database.Tables._
import com.rodneylai.database.Tables.profile.api._
import com.rodneylai.util._

object PostgresAccessHelper {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  m_log.debug("init")

  private val m_configHelperInjector = Guice.createInjector(new ConfigHelperModule)
  private val m_configHelper = m_configHelperInjector.getInstance(classOf[ConfigHelper])

  private lazy val m_postgresUrl:Option[String] = m_configHelper.getString("slick.postgresql.url")

  private lazy val db = Database.forConfig("slick.postgresql")

  def isActive:Boolean = m_postgresUrl.nonEmpty

  def close() = if (isActive) db.close

  def insertToMessageHistory(emailUuid:java.util.UUID,email:String,emailType:String,toEmailAddress:String,now:java.util.Date):Future[Option[java.util.UUID]] = {
    if (isActive) {
      val messageHistoryDao = TableQuery[MessageHistory]

      for {
        result <- db.run(messageHistoryDao += MessageHistoryRow(emailUuid,email,emailType,toEmailAddress,new java.sql.Timestamp(now.getTime)))
      } yield {
        Some(emailUuid)
      }
    } else {
      Future.successful(Some(emailUuid))
    }
  }

  def updateResetPassword(codeUuid:java.util.UUID,emailUuid:java.util.UUID,now:java.util.Date):Future[Option[java.util.UUID]] = {
    if (isActive) {
      val userResetPasswordDao = TableQuery[UserResetPassword]
      val updateMessageId = userResetPasswordDao
                            .filter(userResetPassword => (userResetPassword.userResetPasswordId === codeUuid) && (userResetPassword.messageId.isEmpty))
                            .map(userResetPassword => (userResetPassword.messageId, userResetPassword.updated))
                            .update((Some(emailUuid), new java.sql.Timestamp(now.getTime)))
      val updateStatus = userResetPasswordDao
                            .filter(userResetPassword => (userResetPassword.userResetPasswordId === codeUuid) && (userResetPassword.status === "queued"))
                            .map(userResetPassword => (userResetPassword.status, userResetPassword.updated))
                            .update(("sent", new java.sql.Timestamp(now.getTime)))

      for {
        updatedMessageId <- db.run(updateMessageId)
        updatedStatus <- db.run(updateStatus)
      } yield {
        Some(emailUuid)
      }
    } else {
      Future.successful(Some(emailUuid))
    }
  }

}
