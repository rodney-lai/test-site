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

package com.rodneylai.util

import play.api.mvc._
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure,Success,Try}
import java.util.{Calendar,Date}
import org.mongodb.scala._
import com.rodneylai.models.mongodb._
import com.rodneylai.util._

object TrackingHelper
{
  def getTrackingActionByTypeAndUrl(actionType:String,rawUrl:String)(implicit ctx: ExecutionContext):Future[Option[TrackingAction]] = {
    if (MongoHelper.isActive) {
      for {
        collection <- TrackingActionDao.collectionFuture
        trackingActions <- collection.find(Document("ActionType" -> actionType,"ReferenceType" -> "none", "ReferenceId" -> -1, "RawUrl" -> rawUrl)).toFuture
        result <- trackingActions.headOption match {
          case Some(trackingAction) => Future.successful(TrackingActionDao.fromBson(trackingAction))
          case None => {
            val trackingAction:TrackingAction = TrackingAction( java.util.UUID.randomUUID,
                                                                actionType,
                                                                "none",
                                                                -1,
                                                                rawUrl,
                                                                Calendar.getInstance.getTime)

            for {
              insertResult <- collection.insertOne(TrackingActionDao.toBson(trackingAction)).toFuture
            } yield Some(trackingAction)
          }
        }
      } yield result
    } else {
      Future.successful(None)
    }
  }

  def trackEventByTypeAndUrl(requestHeader:RequestHeader,trackingUuid:java.util.UUID,userUuid:java.util.UUID,actionType:String,rawUrl:String)(implicit ctx: ExecutionContext):Future[Unit] = {
    for {
      trackingActionOption <- TrackingHelper.getTrackingActionByTypeAndUrl(actionType,rawUrl)
      _ <- trackingActionOption match {
        case Some(trackingAction) if (MongoHelper.isActive) => {
          for {
            collection <- TrackingEventDao.collectionFuture
            insertResult <- collection.insertOne(TrackingEventDao.toBson(
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
                requestHeader.headers.get("X-Forwarded-For").getOrElse(requestHeader.remoteAddress),
                requestHeader.cookies.get("PLAY2AUTH_SESS_ID") match {
                  case Some(cookie) => cookie.value
                  case None => ""
                },
                requestHeader.headers.get("User-Agent").getOrElse(""),
                requestHeader.headers.get("Referer").getOrElse(""),
                Calendar.getInstance.getTime
              )
            )).toFuture
          } yield collection
        }
        case _ => Future.successful(Unit)
      }
    } yield Unit
  }
}
