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

package com.rodneylai.util

import play.api.libs.json._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import org.bson.types._

object MongoHelper {
  private val       m_host:String = play.api.Play.current.configuration.getString("mongo.host").getOrElse("")
  private val       m_port:Int = play.api.Play.current.configuration.getInt("mongo.port").getOrElse(27017)
  private val       m_database:String = play.api.Play.current.configuration.getString("mongo.database").getOrElse("")
  private val       m_userName:String = play.api.Play.current.configuration.getString("mongo.user.name").getOrElse("")
  private val       m_password:String = play.api.Play.current.configuration.getString("mongo.password").getOrElse("")
  private val       m_authMechanism:String = play.api.Play.current.configuration.getString("mongo.authmechanism").getOrElse("MONGODB-CR")
  private lazy val  m_server:com.mongodb.ServerAddress = new ServerAddress(m_host, m_port)
  private lazy val  m_mongoCRCredentials:com.mongodb.MongoCredential = MongoCredential.createMongoCRCredential(m_userName, m_database, m_password.toCharArray)
  private lazy val  m_scramSha1Credentials:com.mongodb.MongoCredential = MongoCredential.createScramSha1Credential(m_userName, m_database, m_password.toCharArray)
  private lazy val  m_credentials:Map[String,com.mongodb.MongoCredential] = Map("MONGODB-CR" -> m_mongoCRCredentials, "SCRAM-SHA-1" -> m_scramSha1Credentials)
  private lazy val  m_mongoClient:com.mongodb.casbah.MongoClient = {
                      if ((m_userName.length == 0) || (m_password.length == 0)) {
                        MongoClient(m_server)
                      } else {
                        MongoClient(m_server, List(m_credentials.getOrElse(m_authMechanism,m_mongoCRCredentials)))
                      }
                    }
  private lazy val  m_mongoDB:com.mongodb.casbah.MongoDB = m_mongoClient(m_database)

  object CONSTANTS {
    object UUID {
      val Empty:java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
  }

  val isActive:Boolean = {
      m_host.length > 0
  }

  def getOrCreateCollection(collectionName:String):MongoCollection = {
    m_mongoDB(collectionName)
  }

  def getCollection(collectionName:String):Option[MongoCollection] = {
    if (m_mongoDB.collectionExists(collectionName)) {
      Some(m_mongoDB(collectionName))
    } else {
      None
    }
  }

  def getCollectionList:Option[Set[String]] = {
    if (MongoHelper.isActive) {
      Some(m_mongoDB.collectionNames.toSet)
    } else {
      None
    }
  }

  def getDatabaseStats: Option[Seq[(String,String)]] = {
    if (MongoHelper.isActive) {
      Json.parse(m_mongoDB.getStats.toString).asOpt[JsObject] match {
        case Some(json) => Some(json.value.map({
          case (key,value) => {
            (
              key -> { value match {
                case stringValue:JsString => stringValue.value
                case _ => value.toString
              } }
            )
          }
        }).toSeq.sortBy(_._1))
        case None => Some(Seq[(String,String)]())
      }
    } else {
      None
    }
  }

  def getCollectionStats(collectionName:String): Option[Seq[(String,String)]] = {
    if (MongoHelper.isActive) {
      if (m_mongoDB.collectionExists(collectionName)) {
        Json.parse(m_mongoDB(collectionName).getStats.toString).asOpt[JsObject] match {
          case Some(json) => Some(json.value.map({
            case (key,value) => {
              (
                key -> { value match {
                  case stringValue:JsString => stringValue.value
                  case _ => value.toString
                } }
              )
            }
          }).toSeq.sortBy(_._1))
          case None => Some(Seq[(String,String)]())
        }
      } else {
        None
      }
    } else {
      None
    }
  }

/**
 * Convert a UUID object to a Binary with a subtype 0x04
 * http://3t.io/blog/best-practices-uuid-mongodb/
 */
  def toStandardBinaryUUID(uuid:java.util.UUID):Binary = {
    var msb:Long = uuid.getMostSignificantBits
    var lsb:Long = uuid.getLeastSignificantBits
    val uuidBytes:Array[Byte] = new Array[Byte](16)

    for (i <- 15 to 8 by -1) {
        uuidBytes(i) = (lsb & 0xFFL).toByte
        lsb >>= 8;
    }
    
    for (i <- 7 to 0 by -1) {
        uuidBytes(i) = (msb & 0xFFL).toByte
        msb >>= 8;
    }

    new Binary(0x04, uuidBytes)
  }

/**
 * Convert a Binary with a subtype 0x04 to a UUID object
 * Please note: the subtype is not being checked.
 * http://3t.io/blog/best-practices-uuid-mongodb/
 */
  def fromStandardBinaryUUID(binary:Binary):java.util.UUID = {
    var msb:Long = 0
    var lsb:Long = 0
    val uuidBytes = binary.getData

    for (i <- 8 until 16) {
        lsb <<= 8;
        lsb |= uuidBytes(i) & 0xFFL
    }

    for (i <- 0 until 8) {
        msb <<= 8;
        msb |= uuidBytes(i) & 0xFFL
    }

    new java.util.UUID(msb, lsb)
  }

}

