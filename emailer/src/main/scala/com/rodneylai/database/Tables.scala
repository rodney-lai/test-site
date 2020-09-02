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

import com.google.inject.Guice
import com.rodneylai.util._

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  private val m_configHelperInjector = Guice.createInjector(new ConfigHelperModule)
  private val m_configHelper = m_configHelperInjector.getInstance(classOf[ConfigHelper])

  val schemaName = m_configHelper.getString("postgresql.schemaname").getOrElse("public")

  val profile = slick.jdbc.PostgresProfile

} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val schemaName: String
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = MessageHistory.schema ++ UserResetPassword.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table MessageHistory
   *  @param messageHistoryId Database column Message_History_Id SqlType(uuid), PrimaryKey
   *  @param deliveryMethod Database column Delivery_Method SqlType(varchar), Length(20,true)
   *  @param messageType Database column Message_Type SqlType(varchar), Length(20,true)
   *  @param messageAddress Database column Message_Address SqlType(varchar), Length(256,true)
   *  @param created Database column Created SqlType(timestamptz) */
  case class MessageHistoryRow(messageHistoryId: java.util.UUID, deliveryMethod: String, messageType: String, messageAddress: String, created: java.sql.Timestamp)
  /** GetResult implicit for fetching MessageHistoryRow objects using plain SQL queries */
  implicit def GetResultMessageHistoryRow(implicit e0: GR[java.util.UUID], e1: GR[String], e2: GR[java.sql.Timestamp]): GR[MessageHistoryRow] = GR{
    prs => import prs._
    MessageHistoryRow.tupled((<<[java.util.UUID], <<[String], <<[String], <<[String], <<[java.sql.Timestamp]))
  }
  /** Table description of table Message_History. Objects of this class serve as prototypes for rows in queries. */
  class MessageHistory(_tableTag: Tag) extends profile.api.Table[MessageHistoryRow](_tableTag, Some(schemaName), "Message_History") {
    def * = (messageHistoryId, deliveryMethod, messageType, messageAddress, created) <> (MessageHistoryRow.tupled, MessageHistoryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(messageHistoryId), Rep.Some(deliveryMethod), Rep.Some(messageType), Rep.Some(messageAddress), Rep.Some(created)).shaped.<>({r=>import r._; _1.map(_=> MessageHistoryRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column Message_History_Id SqlType(uuid), PrimaryKey */
    val messageHistoryId: Rep[java.util.UUID] = column[java.util.UUID]("Message_History_Id", O.PrimaryKey)
    /** Database column Delivery_Method SqlType(varchar), Length(20,true) */
    val deliveryMethod: Rep[String] = column[String]("Delivery_Method", O.Length(20,varying=true))
    /** Database column Message_Type SqlType(varchar), Length(20,true) */
    val messageType: Rep[String] = column[String]("Message_Type", O.Length(20,varying=true))
    /** Database column Message_Address SqlType(varchar), Length(256,true) */
    val messageAddress: Rep[String] = column[String]("Message_Address", O.Length(256,varying=true))
    /** Database column Created SqlType(timestamptz) */
    val created: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("Created")
  }
  /** Collection-like TableQuery object for table MessageHistory */
  lazy val MessageHistory = new TableQuery(tag => new MessageHistory(tag))

  /** Entity class storing rows of table UserResetPassword
   *  @param userResetPasswordId Database column User_Reset_Password_Id SqlType(uuid), PrimaryKey
   *  @param userId Database column User_Id SqlType(int8)
   *  @param status Database column Status SqlType(varchar), Length(20,true)
   *  @param messageId Database column Message_Id SqlType(uuid), Default(None)
   *  @param updated Database column Updated SqlType(timestamptz)
   *  @param created Database column Created SqlType(timestamptz) */
  case class UserResetPasswordRow(userResetPasswordId: java.util.UUID, userId: Long, status: String, messageId: Option[java.util.UUID] = None, updated: java.sql.Timestamp, created: java.sql.Timestamp)
  /** GetResult implicit for fetching UserResetPasswordRow objects using plain SQL queries */
  implicit def GetResultUserResetPasswordRow(implicit e0: GR[java.util.UUID], e1: GR[Long], e2: GR[String], e3: GR[Option[java.util.UUID]], e4: GR[java.sql.Timestamp]): GR[UserResetPasswordRow] = GR{
    prs => import prs._
    UserResetPasswordRow.tupled((<<[java.util.UUID], <<[Long], <<[String], <<?[java.util.UUID], <<[java.sql.Timestamp], <<[java.sql.Timestamp]))
  }
  /** Table description of table User_Reset_Password. Objects of this class serve as prototypes for rows in queries. */
  class UserResetPassword(_tableTag: Tag) extends profile.api.Table[UserResetPasswordRow](_tableTag, Some(schemaName), "User_Reset_Password") {
    def * = (userResetPasswordId, userId, status, messageId, updated, created) <> (UserResetPasswordRow.tupled, UserResetPasswordRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(userResetPasswordId), Rep.Some(userId), Rep.Some(status), messageId, Rep.Some(updated), Rep.Some(created)).shaped.<>({r=>import r._; _1.map(_=> UserResetPasswordRow.tupled((_1.get, _2.get, _3.get, _4, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column User_Reset_Password_Id SqlType(uuid), PrimaryKey */
    val userResetPasswordId: Rep[java.util.UUID] = column[java.util.UUID]("User_Reset_Password_Id", O.PrimaryKey)
    /** Database column User_Id SqlType(int8) */
    val userId: Rep[Long] = column[Long]("User_Id")
    /** Database column Status SqlType(varchar), Length(20,true) */
    val status: Rep[String] = column[String]("Status", O.Length(20,varying=true))
    /** Database column Message_Id SqlType(uuid), Default(None) */
    val messageId: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("Message_Id", O.Default(None))
    /** Database column Updated SqlType(timestamptz) */
    val updated: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("Updated")
    /** Database column Created SqlType(timestamptz) */
    val created: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("Created")
  }
  /** Collection-like TableQuery object for table UserResetPassword */
  lazy val UserResetPassword = new TableQuery(tag => new UserResetPassword(tag))
}
