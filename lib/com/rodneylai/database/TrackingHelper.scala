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

package com.rodneylai.database

import play.api.mvc._
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure,Success,Try}
import java.util.{Calendar,Date}
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.models.mongodb._
import com.rodneylai.util._

@Singleton
class TrackingHelper @Inject() (mongoHelper:MongoHelper,trackingActionDao:TrackingActionDao,trackingEventDao:TrackingEventDao)
{
  private val       m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  def getTrackingActionByTypeAndUrl(actionType:String,rawUrl:String)(implicit ctx: ExecutionContext):Future[Option[TrackingAction]] = {
    if (mongoHelper.isActive) {
      for {
        collection <- trackingActionDao.collectionFuture
        trackingActions <- collection.find(Document("ActionType" -> actionType,"ReferenceType" -> "none", "ReferenceId" -> -1, "RawUrl" -> rawUrl)).toFuture
        result <- trackingActions.headOption match {
          case Some(trackingAction) => Future.successful(trackingActionDao.fromBson(trackingAction))
          case None => {
            val trackingAction:TrackingAction = TrackingAction( java.util.UUID.randomUUID,
                                                                actionType,
                                                                "none",
                                                                -1,
                                                                rawUrl,
                                                                Calendar.getInstance.getTime)

            for {
              insertResult <- collection.insertOne(trackingActionDao.toBson(trackingAction)).toFuture
            } yield Some(trackingAction)
          }
        }
      } yield result
    } else {
      Future.successful(None)
    }
  }

  def trackEventByTypeAndUrl(requestHeader:RequestHeader,trackingUuid:java.util.UUID,userUuid:java.util.UUID,actionType:String,rawUrl:String)(implicit ctx: ExecutionContext):Future[Option[Long]] = {
    def LongOption(value:Long):Option[Long] = if (value > 0) Some(value) else None

    for {
      trackingActionOption <- getTrackingActionByTypeAndUrl(actionType,rawUrl)
      trackingCountOption <- trackingActionOption match {
        case Some(trackingAction) if (mongoHelper.isActive) => {
          val dateLongOffset:Calendar = Calendar.getInstance
          val dateShortOffset:Calendar = Calendar.getInstance
          val ipAddress:String = requestHeader.headers.get("X-Forwarded-For").getOrElse(requestHeader.remoteAddress)

          dateLongOffset.add(Calendar.DATE,-7)
          dateShortOffset.add(Calendar.DATE,-1)
          for {
            collection <- trackingEventDao.collectionFuture
            trackingCountResult0 <- if (trackingAction.actionType == "page_not_found") {
              collection.count(
                and(
                  equal("ActionUuid", MongoHelper.toStandardBinaryUUID(trackingAction.actionUuid)),
                  gt("CreateDate", dateLongOffset.getTime)
                )
              ).toFuture
            } else {
              Future.successful(Nil)
            }
            trackingCountResult1 <- if (trackingAction.actionType == "page_not_found") {
              collection.count(
                and(
                  equal("ActionType","page_not_found"),
                  equal("IpAddress", ipAddress),
                  gt("CreateDate", dateShortOffset.getTime)
                )
              ).toFuture
            } else {
              Future.successful(Nil)
            }
            insertResult <- collection.insertOne(trackingEventDao.toBson(
              TrackingEvent(
                trackingUuid,
                requestHeader.cookies.get("tracking_source_id") match {
                  case Some(trackingSourceIdCookie) => {
                    Try(java.util.UUID.fromString(trackingSourceIdCookie.value)) match {
                      case Success(trackingSourceId) => trackingSourceId
                      case Failure(ex) => MongoHelper.CONSTANTS.UUID.Empty
                    }
                  }
                  case None => MongoHelper.CONSTANTS.UUID.Empty
                },
                actionType,
                trackingAction.actionUuid,
                userUuid,
                ipAddress,
                requestHeader.cookies.get("PLAY2AUTH_SESS_ID") match {
                  case Some(cookie) => cookie.value
                  case None => ""
                },
                requestHeader.headers.get("User-Agent").getOrElse(""),
                requestHeader.headers.get("Referer").getOrElse(""),
                Calendar.getInstance.getTime
              )
            )).toFuture
          } yield {
            List(trackingCountResult0.headOption,trackingCountResult1.headOption).flatten match {
              case Nil => None
              case xs => LongOption(xs.max)
            }
          }
        }
        case _ => Future.successful(None)
      }
    } yield trackingCountOption
  }
}

class TrackingHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[TrackingHelper]).asEagerSingleton
  }
}
