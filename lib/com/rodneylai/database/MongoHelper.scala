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

import play.api.libs.json._
import play.libs.Scala
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonBinary}
import org.mongodb.scala.connection._
import org.slf4j.{Logger,LoggerFactory}

object MongoHelper {

  object CONSTANTS {
    object UUID {
      val Empty:java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
  }

  /**
   * Convert a UUID object to a Binary with a subtype 0x04
   * http://3t.io/blog/best-practices-uuid-mongodb/
   */
    def toStandardBinaryUUID(uuid:java.util.UUID):BsonBinary = {
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

      new org.bson.BsonBinary(org.bson.BsonBinarySubType.UUID_STANDARD, uuidBytes)
    }

  /**
   * Convert a Binary with a subtype 0x04 to a UUID object
   * Please note: the subtype is not being checked.
   * http://3t.io/blog/best-practices-uuid-mongodb/
   */
    def fromStandardBinaryUUID(uuidBytes:Array[Byte]):java.util.UUID = {
      var msb:Long = 0
      var lsb:Long = 0

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

@Singleton
class MongoHelper @Inject() (configuration:play.api.Configuration) {
  private val       m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  private val       m_host:String = configuration.getString("mongo.host").getOrElse("")
  private val       m_port:Int = configuration.getInt("mongo.port").getOrElse(27017)
  private val       m_database:String = configuration.getString("mongo.database").getOrElse("")
  private val       m_userName:String = configuration.getString("mongo.user.name").getOrElse("")
  private val       m_password:String = configuration.getString("mongo.password").getOrElse("")
  private val       m_authMechanism:String = configuration.getString("mongo.authmechanism").getOrElse("MONGODB-CR")
  private lazy val  m_server:ServerAddress = new ServerAddress(m_host, m_port)
  private lazy val  m_clusterSettings:ClusterSettings = ClusterSettings.builder().hosts(Scala.asJava(List(m_server))).build()
  private lazy val  m_mongoCRCredentials:MongoCredential = MongoCredential.createMongoCRCredential(m_userName, m_database, m_password.toCharArray)
  private lazy val  m_scramSha1Credentials:MongoCredential = MongoCredential.createScramSha1Credential(m_userName, m_database, m_password.toCharArray)
  private lazy val  m_credentials:Map[String,MongoCredential] = Map("MONGODB-CR" -> m_mongoCRCredentials, "SCRAM-SHA-1" -> m_scramSha1Credentials)
  private lazy val  m_mongoClient:MongoClient = {
                      if ((m_userName.isEmpty) || (m_password.isEmpty)) {
                        val m_settings:MongoClientSettings = MongoClientSettings.builder().clusterSettings(m_clusterSettings).build()

                        MongoClient(m_settings)
                      } else {
                        val m_settings:MongoClientSettings = MongoClientSettings.builder().clusterSettings(m_clusterSettings).credentialList(Scala.asJava(List(m_credentials.getOrElse(m_authMechanism,m_mongoCRCredentials)))).build()

                        MongoClient(m_settings)
                      }
                    }
  private val       m_mongoDatabase:MongoDatabase = m_mongoClient.getDatabase(m_database)

  val isActive:Boolean = {
    m_host.length > 0
  }

  def getCollection(collectionName:String):MongoCollection[Document] = {
    m_mongoDatabase.getCollection(collectionName)
  }

  def getCollectionList:Future[Option[Set[String]]] = {
    if (isActive) {
      for {
        collectionNames <- m_mongoDatabase.listCollectionNames.toFuture
      } yield Some(collectionNames.toSet)
    } else {
      Future.successful(None)
    }
  }

  def getDatabaseStats:Future[Option[Seq[(String,String)]]] = {
    if (isActive) {
      for {
        result <- m_mongoDatabase.runCommand(Document("dbStats" -> 1 )).toFuture
      } yield {
        result.headOption match {
          case Some(document) => {
            Json.parse(document.toJson.toString).asOpt[JsObject] match {
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
          }
          case None => None
        }
      }
    } else {
      Future.successful(None)
    }
  }

  def getCollectionStats(collectionName:String):Future[Option[Seq[(String,String)]]] = {
    if (isActive) {
      for {
        result <- m_mongoDatabase.runCommand(Document("collStats" -> collectionName )).toFuture
      } yield {
        result.headOption match {
          case Some(document) => {
            Json.parse(document.toJson.toString).asOpt[JsObject] match {
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
          }
          case None => None
        }
      }
    } else {
      Future.successful(None)
    }
  }

}

class MongoHelperModule extends AbstractModule {
  def configure() = {
    bind(classOf[MongoHelper]).asEagerSingleton
  }
}
