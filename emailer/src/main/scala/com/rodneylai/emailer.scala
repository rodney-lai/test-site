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

package com.rodneylai

import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure,Success,Try}
import java.util.Calendar
import java.util.concurrent.{ArrayBlockingQueue,ThreadPoolExecutor,TimeUnit}
import com.fasterxml.jackson.annotation.{JsonTypeInfo,JsonSubTypes}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.databind.{DeserializationFeature,ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.Guice
import com.redis._
import org.apache.commons.mail._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.amazon.s3._
import com.rodneylai.database._
import com.rodneylai.util._

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "cmd")
@JsonSubTypes(value = Array(
    new Type(value = classOf[ResetPasswordEmailQueue], name = "reset-password"),
    new Type(value = classOf[InviteEmailQueue], name = "invite")
))
trait EmailQueueCmd

trait EmailQueue {
  def toEmailAddress:String
  def fromName:Option[String]
  def data:Option[Map[String,Any]]
}

case class ResetPasswordEmailQueue(val toEmailAddress:String,val fromName:Option[String],val data:Option[Map[String,Any]],code:java.util.UUID,baseUrl:String,now:java.util.Date) extends EmailQueueCmd with EmailQueue
case class InviteEmailQueue(val toEmailAddress:String,val fromName:Option[String],val data:Option[Map[String,Any]],code:String,baseUrl:String,now:java.util.Date) extends EmailQueueCmd with EmailQueue

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
  private val m_emailToStartupOption:Option[String] = m_configHelper.getString("email.to.startup")

  private val m_threadPoolSize:Int = m_configHelper.getInt("thread.pool.size").getOrElse(10)
  private val m_threadQueueCapacity:Int = m_configHelper.getInt("thread.queue.capacity").getOrElse(50)
  private val m_useExecutionContext:Boolean = m_configHelper.getBoolean("use.execution.context").getOrElse(true)

  private val m_mongoAccessHelperInjector = Guice.createInjector(new MongoAccessHelperModule)
  private val m_mongoAccessHelper = m_mongoAccessHelperInjector.getInstance(classOf[MongoAccessHelper])

  private val m_s3HelperInjector = Guice.createInjector(new S3HelperModule)
  private val m_s3Helper = m_s3HelperInjector.getInstance(classOf[S3Helper])

  private def getEmailTemplateNames(emailType:String) = Array(s"$emailType-subject.txt",s"$emailType.txt",s"$emailType.html")

  private val m_templates:Array[String] = getEmailTemplateNames("invite") ++ getEmailTemplateNames("reset-password")

  private def getInfo(): String = {
    val buildDate = Try(io.Source.fromFile("./BUILD_DATE").getLines.mkString("\n")) match {
      case Success(result) => result
      case Failure(ex) => "unknown"
    }
    val scalaVersion = scala.util.Properties.versionNumberString
    val machineName = java.net.InetAddress.getLocalHost.getHostName
    val machineIp = java.net.InetAddress.getLocalHost.getHostAddress

    s"BuildDate: $buildDate\nScalaVersion: $scalaVersion\nMachineName: $machineName\nMachineIp: $machineIp"
  }

  private def sendStartupEmail(): Boolean = {
    (m_emailHostOption, m_emailToStartupOption) match {
      case (Some(emailHost), Some(emailToStartup)) =>
        val email = new SimpleEmail()

        email.setHostName(emailHost)
        email.setSmtpPort(m_emailPort)
        (m_emailUserNameOption,m_emailPasswordOption) match {
          case (Some(emailUserName),Some(emailPassword)) =>
            email.setAuthenticator(new DefaultAuthenticator(emailUserName, emailPassword))
          case _ =>
        }
        email.setSSLOnConnect(true)
        m_emailFromEmailOption.map { emailFromEmail =>
          email.setFrom(emailFromEmail)
        }
        email.setSubject("Emailer Startup")
        email.setMsg(getInfo())
        email.addTo(emailToStartup)
        email.setDebug(true)
        Try(email.send()) match {
          case Success(result) =>
            true
          case Failure(ex) =>
            m_log.error(s"sendStartupEmail[send_failed]",ex)
            false
        }
      case _ =>
        m_log.error(s"sendStartupEmail[not_configured]")
        false
    }
  }

  private def sendEmail(emailQueue:EmailQueue,emailType:String,subjectEmailTemplateOption:Option[String],txtEmailTemplateOption:Option[String],htmlEmailTemplateOption:Option[String],values:Map[String,Any],now:java.util.Date):Future[Option[java.util.UUID]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
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
          (m_emailFromEmailOption,emailQueue.fromName.orElse(m_emailFromNameOption)) match {
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
          email.addTo(emailQueue.toEmailAddress)
          email.setDebug(true)
          Try(email.send()) match {
            case Success(result) => {
              val mongoFuture = m_mongoAccessHelper.insertToMessageLog(emailUuid,"email",emailType,emailQueue.toEmailAddress,now)
              val postgresFuture = PostgresAccessHelper.insertToMessageHistory(emailUuid,"email",emailType,emailQueue.toEmailAddress,now)

              for {
                mongoResult <- mongoFuture
                postgresResult <- postgresFuture
              } yield {
                Some(emailUuid)
              }
            }
            case Failure(ex) =>
              m_log.error(s"sendEmail[$emailQueue.toEmailAddress]",ex)
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

  private def sendEmailTemplate(emailType:String,templates:Map[String,String],emailQueue:EmailQueue,params:Map[String,Any],now:java.util.Date):Future[Option[java.util.UUID]] = {
    val subjectEmailTemplateOption:Option[String] = templates.get(s"$emailType-subject.txt").orElse(readResourceFile(s"/email-templates/$emailType-subject.txt"))
    val txtEmailTemplateOption:Option[String] = templates.get(s"$emailType.txt").orElse(readResourceFile(s"/email-templates/$emailType.txt"))
    val htmlEmailTemplateOption:Option[String] = templates.get(s"$emailType.html").orElse(readResourceFile(s"/email-templates/$emailType.html"))

    sendEmail(emailQueue,emailType,subjectEmailTemplateOption,txtEmailTemplateOption,htmlEmailTemplateOption,params,now)
  }

  private def readResourceFile(resourcePath: String): Option[String] = {
    Option(getClass.getResourceAsStream(resourcePath))
      .map(scala.io.Source.fromInputStream)
      .map(_.getLines().toList.mkString("\n"))
  }

  private def processCmd(json:String,templates:Map[String,String]):Future[Option[(String,String,java.util.UUID)]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val now:java.util.Date = Calendar.getInstance.getTime

    val objectMapper = new ObjectMapper() with ScalaObjectMapper
    objectMapper.registerModule(DefaultScalaModule)
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    Try(objectMapper.readValue[EmailQueueCmd](json)) match {
      case Success(inviteEmailQueue:InviteEmailQueue) => {
        m_log.debug(s"invite[$inviteEmailQueue]")
        for {
          emailUuidOption <- sendEmailTemplate("invite",templates,inviteEmailQueue,(convertCaseClassToMap(inviteEmailQueue) - "data") ++ inviteEmailQueue.data.getOrElse(Nil),now)
        } yield {
          emailUuidOption map { emailUuid => ("invite",inviteEmailQueue.toEmailAddress,emailUuid) }
        }
      }
      case Success(resetPasswordEmailQueue:ResetPasswordEmailQueue) => {
        m_log.debug(s"reset-password[$resetPasswordEmailQueue]")
        for {
          emailUuidOption <- sendEmailTemplate("reset-password",templates,resetPasswordEmailQueue,(convertCaseClassToMap(resetPasswordEmailQueue) - "data") ++ resetPasswordEmailQueue.data.getOrElse(Nil),now)
          result <- emailUuidOption match {
            case Some(emailUuid) => {
              val mongoFuture = m_mongoAccessHelper.updateResetPassword(resetPasswordEmailQueue.code,emailUuid,now)
              val postgresFuture = PostgresAccessHelper.updateResetPassword(resetPasswordEmailQueue.code,emailUuid,now)

              for {
                mongoResult <- mongoFuture
                postgresResult <- postgresFuture
              } yield {
                Some(emailUuid)
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
  }

  def doProcessCmd(json:String,templates:Map[String,String]):Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    for {
      processCmdResult <- processCmd(json,templates)
    } yield {
      processCmdResult match {
        case Some((cmd:String,toEmailAddress:String,emailUuid:java.util.UUID)) => m_log.debug(s"success[$cmd][$toEmailAddress][$emailUuid]")
        case None => m_log.error(s"fail[$json]")
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val threadPoolExecutor = new ThreadPoolExecutor(
                                m_threadPoolSize, m_threadPoolSize,
                                0L, TimeUnit.SECONDS,
                                new ArrayBlockingQueue[Runnable](m_threadQueueCapacity) {
                                  override def offer(e: Runnable) = {
                                    put(e); // may block if waiting for empty room
                                    true
                                  }
                                }
                              )
    val templates:Map[String,String] = m_s3Helper.getS3TextFiles(m_templates) match {
      case Some(templates) => m_templates.zip(templates).flatMap {
        case (templateName,Some(templateContent)) => Some((templateName,templateContent))
        case (templateName,None) => None
      }.toMap
      case None => Map.empty
    }

    try {
      val redis = new RedisClient(m_redisHost,m_redisPort,secret = m_redisPassword)
      implicit val ec = ExecutionContext.fromExecutorService(threadPoolExecutor)

      sendStartupEmail()
      while (true) {
        val cmd = redis.brpop(1,"email-queue")

        if (m_useExecutionContext) {
          Future {
            cmd.map({
              case (name,value) => doProcessCmd(value,templates)
            })
          }
        } else {
          cmd.map({
            case (name,value) => doProcessCmd(value,templates)
          })
        }
      }
    } finally {
      threadPoolExecutor.shutdown()
      PostgresAccessHelper.close()
    }

  }
}
