/**
 *
 * Copyright (c) 2015-2016 Rodney S.K. Lai
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

package com.rodneylai

import scala.io._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await,ExecutionContext,Future}
import scala.reflect.runtime.universe._
import scala.util.{Failure,Success,Try}
import java.util.{Calendar,Date}
import com.fasterxml.jackson.annotation.{JsonTypeInfo,JsonSubTypes,JsonProperty}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.databind.{DeserializationFeature,ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.Guice
import com.redis._
import org.apache.commons.mail._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.database._
import com.rodneylai.models.mongodb._
import com.rodneylai.util._

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "cmd")
@JsonSubTypes(value = Array(
    new Type(value = classOf[ResetPasswordEmailQueue], name = "reset-password")
))
trait EmailQueueCmd

trait EmailQueue {
  def toEmailAddress:String
}

case class ResetPasswordEmailQueue(val toEmailAddress:String,code:java.util.UUID,baseUrl:String,now:java.util.Date) extends EmailQueueCmd with EmailQueue

object emailer {
  private val m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  private def buildMsg(emailTemplate:String,values:Map[String,Any]):String = {
    if (values.isEmpty) {
      return(emailTemplate)
    } else {
      return(buildMsg(emailTemplate.replace("@(" + values.head._1.toString + ")",values.head._2.toString),values.tail))
    }
  }

  private def convertCaseClassToMap(caseClass: Product):Map[String,Any] = {
    caseClass.getClass
      .getDeclaredFields.map(_.getName)   // all field names
      .zip(caseClass.productIterator.to)  // zipped with all values
      .toMap
  }

  private val m_configHelperInjector = Guice.createInjector(new ConfigHelperModule)
  private val m_configHelper = m_configHelperInjector.getInstance(classOf[ConfigHelper])

  private val m_redisHost:String = m_configHelper.getString("redis.host").getOrElse("localhost")
  private val m_redisPort:Int = m_configHelper.getInt("redis.port").getOrElse(6379)
  private val m_redisPassword:Option[String] = m_configHelper.getString("redis.password")

  private val m_emailHostOption:Option[String] = m_configHelper.getString("email.host")
  private val m_emailPort:Int = m_configHelper.getInt("email.port").getOrElse(465)
  private val m_emailUserNameOption:Option[String] = m_configHelper.getString("email.username")
  private val m_emailPasswordOption:Option[String] = m_configHelper.getString("email.password")
  private val m_emailFromEmailOption:Option[String] = m_configHelper.getString("email.from.email")
  private val m_emailFromNameOption:Option[String] = m_configHelper.getString("email.from.name")

  private def sendEmail(toEmailAddress:String,emailType:String,subjectEmailTemplateOption:Option[String],txtEmailTemplateOption:Option[String],htmlEmailTemplateOption:Option[String],values:Map[String,Any],now:java.util.Date):Future[Option[java.util.UUID]] = {
    (m_emailHostOption,subjectEmailTemplateOption) match {
      case (Some(emailHost),Some(subjectEmailTemplate)) if ((htmlEmailTemplateOption.isDefined) || (txtEmailTemplateOption.isDefined)) => {
        val emailUuid:java.util.UUID = java.util.UUID.randomUUID
        val subject:String = buildMsg(subjectEmailTemplate,values)
        val txtEmailContentOption:Option[String] = txtEmailTemplateOption.map { txtEmailTemplate => buildMsg(txtEmailTemplate,values + ("emailId" -> emailUuid)) }
        val htmlEmailContentOption:Option[String] = htmlEmailTemplateOption.map { htmlEmailTemplate => buildMsg(htmlEmailTemplate,values + ("emailId" -> emailUuid)) }

        if (((subject.contains("@("))) || (htmlEmailContentOption.map(_.contains("@(")).getOrElse(false)) || (txtEmailContentOption.map(_.contains("@(")).getOrElse(false))) {
          m_log.error(s"invalid template for email type = $emailType")
          Future.successful(None)
        } else {
          val htmlEmail:HtmlEmail = if (htmlEmailContentOption.isDefined) new HtmlEmail() else null
          val simpleEmail:SimpleEmail = if (htmlEmailContentOption.isDefined) null else new SimpleEmail()
          val email:Email = if (htmlEmail != null) htmlEmail else simpleEmail

          email.setHostName(emailHost)
          email.setSmtpPort(m_emailPort)
          (m_emailUserNameOption,m_emailPasswordOption) match {
            case (Some(emailUserName),Some(emailPassword)) => email.setAuthenticator(new DefaultAuthenticator(emailUserName, emailPassword))
            case _ => {}
          }
          email.setSSLOnConnect(true)
          (m_emailFromEmailOption,m_emailFromNameOption) match {
            case (Some(emailFromEmail),Some(emailFromName)) => email.setFrom(emailFromEmail,emailFromName)
            case (Some(emailFromEmail),None) => email.setFrom(emailFromEmail)
            case _ => {}
          }
          email.setSubject(subject)
          if (htmlEmailContentOption.isDefined) {
            txtEmailContentOption.map { htmlEmail.setTextMsg(_) }
            htmlEmailContentOption.map { htmlEmail.setHtmlMsg(_) }
          } else if (txtEmailContentOption.isDefined) {
            txtEmailContentOption.map { email.setMsg(_) }
          }
          email.addTo(toEmailAddress);
          email.setDebug(true)
          Try(email.send()) match {
            case Success(result) => {
              val messageLogDaoInjector = Guice.createInjector(new MessageLogDaoModule)
              val messageLogDao = messageLogDaoInjector.getInstance(classOf[MessageLogDao])
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
            }
            case Failure(ex) =>
              m_log.error(s"sendEmail[$toEmailAddress]",ex)
              Future.successful(None)
          }
        }
      }
      case _ => {
        if (!m_emailHostOption.isDefined) m_log.error("email.host not configured")
        if (!subjectEmailTemplateOption.isDefined) m_log.error(s"missing subject template for email type = $emailType")
        Future.successful(None)
      }
    }
  }

  private def sendEmailTemplate(emailType:String,toEmailAddress:String,params:Map[String,Any],now:java.util.Date):Future[Option[java.util.UUID]] = {
    val subjectEmailTemplateOption:Option[String] = readResourceFile(s"/email-templates/$emailType-subject.txt")
    val txtEmailTemplateOption:Option[String] = readResourceFile(s"/email-templates/$emailType.txt")
    val htmlEmailTemplateOption:Option[String] = readResourceFile(s"/email-templates/$emailType.html")

    sendEmail(toEmailAddress,emailType,subjectEmailTemplateOption,txtEmailTemplateOption,htmlEmailTemplateOption,params,now)
  }

  private def readResourceFile(resourcePath: String): Option[String] = {
    Option(getClass.getResourceAsStream(resourcePath))
      .map(scala.io.Source.fromInputStream)
      .map(_.getLines.toList.mkString("\n"))
  }

  def main(args: Array[String]): Unit = {
    val redis = new RedisClient(m_redisHost,m_redisPort,secret = m_redisPassword)

    while (true) {
      val cmd = redis.brpop(1,"email-queue")
      cmd.map({
        case (name,value) => {
          val now:java.util.Date = Calendar.getInstance.getTime

          val objectMapper = new ObjectMapper() with ScalaObjectMapper
          objectMapper.registerModule(DefaultScalaModule)
          objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

          val processCmd:Future[Option[(String,String,java.util.UUID)]] = Try(objectMapper.readValue[EmailQueueCmd](value)) match {
            case Success(resetPasswordEmailQueue:ResetPasswordEmailQueue) => {
              val resetPasswordDaoInjector = Guice.createInjector(new ResetPasswordDaoModule)
              val resetPasswordDao = resetPasswordDaoInjector.getInstance(classOf[ResetPasswordDao])

              m_log.debug(s"reset-password[$resetPasswordEmailQueue]")
              for {
                emailUuidOption <- sendEmailTemplate("reset-password",resetPasswordEmailQueue.toEmailAddress,convertCaseClassToMap(resetPasswordEmailQueue),now)
                result <- emailUuidOption match {
                  case Some(emailUuid) => {
                    for {
                      collection <- resetPasswordDao.collectionFuture
                      updateMessageUuidResult <- collection.updateOne(
                        and(
                          equal("CodeUuid",MongoHelper.toStandardBinaryUUID(resetPasswordEmailQueue.code)),
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
                          equal("CodeUuid",MongoHelper.toStandardBinaryUUID(resetPasswordEmailQueue.code)),
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
                      emailUuid
                    }
                  }
                  case None => Future.successful(None)
                }
              } yield {
                emailUuidOption map { emailUuid => ("reset-password",resetPasswordEmailQueue.toEmailAddress,emailUuid) }
              }
            }
            case Success(cmd) => {
              m_log.error(s"unknown_cmd[$cmd]")
              Future.successful(None)
            }
            case Failure(ex) => {
              m_log.error("parse_json",ex)
              Future.successful(None)
            }
          }
          Await.result(processCmd,Duration.Inf) match {
            case Some((cmd:String,toEmailAddress:String,emailUuid:java.util.UUID)) => m_log.debug(s"success[$cmd][$toEmailAddress][$emailUuid]")
            case None => m_log.error(s"fail[$value]")
          }
        }
      })
    }

  }
}
