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
import javax.inject.{Inject,Singleton}
import com.google.inject.AbstractModule
import com.mongodb.Block
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonBinary,BsonBoolean,BsonDouble,BsonInt32,BsonString}
import org.mongodb.scala.connection._
import org.slf4j.{Logger,LoggerFactory}
import com.rodneylai.util._

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
class MongoHelper @Inject() (
  configHelper:ConfigHelper,
  conversionHelper:ConversionHelper
) {
  private val       m_log:Logger = LoggerFactory.getLogger(this.getClass.getName)

  m_log.debug("init")

  private val       m_url:String = configHelper.getString("mongo.url").getOrElse("")
  private val       m_host:String = configHelper.getString("mongo.host").getOrElse("")
  private val       m_port:Int = configHelper.getInt("mongo.port").getOrElse(27017)
  private val       m_database:String = configHelper.getString("mongo.database").getOrElse("")
  private val       m_userName:String = configHelper.getString("mongo.username").getOrElse("")
  private val       m_password:String = configHelper.getString("mongo.password").getOrElse("")
  private val       m_authMechanism:String = configHelper.getString("mongo.authmechanism").getOrElse("MONGODB-CR")
  private lazy val  m_server:ServerAddress = new ServerAddress(m_host, m_port)
  private lazy val  m_scramSha1Credentials:MongoCredential = MongoCredential.createScramSha1Credential(m_userName, m_database, m_password.toCharArray)
  private lazy val  m_scramSha256Credentials:MongoCredential = MongoCredential.createScramSha256Credential(m_userName, m_database, m_password.toCharArray)
  private lazy val  m_credentials:Map[String,MongoCredential] = Map(
    "SCRAM-SHA-1" -> m_scramSha1Credentials,
    "SCRAM-SHA-256" -> m_scramSha256Credentials
  )
  private lazy val  m_mongoClient:MongoClient = {
    if (m_url.nonEmpty) {
      System.setProperty("org.mongodb.async.type", "netty")
      MongoClient(m_url)
    } else if ((m_userName.isEmpty) || (m_password.isEmpty)) {
      val m_settings:MongoClientSettings = MongoClientSettings.builder()
        .applyToClusterSettings(
          new Block[ClusterSettings.Builder]() {
            override def apply(builder: ClusterSettings.Builder): Unit = builder.hosts(conversionHelper.asJavaList(List(m_server)))
          }
        )
        .retryWrites(false)
        .build()

      MongoClient(m_settings)
    } else {
      val m_settings:MongoClientSettings = MongoClientSettings.builder()
        .applyToClusterSettings(
          new Block[ClusterSettings.Builder]() {
            override def apply(builder: ClusterSettings.Builder): Unit = builder.hosts(conversionHelper.asJavaList(List(m_server)))
          }
        )
        .credential(m_credentials.getOrElse(m_authMechanism,m_scramSha1Credentials))
        .retryWrites(false)
        .build()

      MongoClient(m_settings)
    }
  }
  private lazy val  m_mongoDatabase:MongoDatabase = m_mongoClient.getDatabase(m_database)

  val isActive:Boolean = {
    (m_host.length > 0) || (m_url.length > 0)
  }

  def close():Unit = {
    m_mongoClient.close()
  }

  def getCollection(collectionName:String):MongoCollection[Document] = {
    m_mongoDatabase.getCollection(collectionName)
  }

  def getCollectionList:Future[Option[Set[String]]] = {
    if (isActive) {
      for {
        collectionNames <- m_mongoDatabase.listCollectionNames().toFuture()
      } yield Some(collectionNames.toSet)
    } else {
      Future.successful(None)
    }
  }

  def getDatabaseStats:Future[Option[Seq[(String,String)]]] = {
    if (isActive) {
      for {
        result <- m_mongoDatabase.runCommand(Document("dbStats" -> 1 )).toFuture()
      } yield {
        Some((result map {
          case (key,value) => {
            key -> { value match {
              case stringValue:BsonString => stringValue.getValue
              case booleanValue:BsonBoolean => booleanValue.getValue.toString
              case int32Value:BsonInt32 => int32Value.getValue.toString
              case doubleValue:BsonDouble => doubleValue.getValue.toString
              case _ => value.toString
            } }
          }
        }).toSeq.sortBy(_._1))
      }
    } else {
      Future.successful(None)
    }
  }

  def getCollectionStats(collectionName:String):Future[Option[Seq[(String,String)]]] = {
    if (isActive) {
      for {
        result <- m_mongoDatabase.runCommand(Document("collStats" -> collectionName )).toFuture()
      } yield {
        Some((result map {
          case (key,value) => {
            key -> { value match {
              case stringValue:BsonString => stringValue.getValue
              case booleanValue:BsonBoolean => booleanValue.getValue.toString
              case int32Value:BsonInt32 => int32Value.getValue.toString
              case doubleValue:BsonDouble => doubleValue.getValue.toString
              case _ => value.toString
            } }
          }
        }).toSeq.sortBy(_._1))
      }
    } else {
      Future.successful(None)
    }
  }

}

class MongoHelperModule extends AbstractModule {
  override def configure() = {
    bind(classOf[MongoHelper]).asEagerSingleton
  }
}
