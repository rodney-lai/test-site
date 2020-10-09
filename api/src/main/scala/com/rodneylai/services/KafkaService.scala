/**
 *
 * Copyright (c) 2020 Rodney S.K. Lai
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

package com.rodneylai.services

import com.google.inject.{AbstractModule, Inject}
import org.slf4j.LoggerFactory
import io.circe.generic.auto._
import io.circe.parser
import pureconfig._
import pureconfig.generic.auto._
import zio._
import zio.kafka.consumer._
import zio.kafka.serde._
import zio.stream.ZStream

trait KafkaService {
  def getWebCamStream(): ZStream[Any, Throwable, Option[String]]
}

class KafkaServiceImpl @Inject() (
) extends KafkaService {
  private val log = LoggerFactory.getLogger(this.getClass.getName)

  case class KafkaConfig(
    bootstrapServers: Option[String],
    user: Option[String],
    password: Option[String],
    webcamKey: Option[String]
  )

  case class WebCamImage(key: String, dateCreated: String)

  def getWebCamStream(): ZStream[Any, Throwable, Option[String]] = ZStream.unwrap {
    ConfigSource.default.at("kafka").load[KafkaConfig] match {
      case Right(config) =>
        (config.bootstrapServers, config.user, config.password, config.webcamKey) match {
          case (Some(bootstrapServers),Some(user),Some(password),Some(webcamKey)) =>
            Task {
              val startTime = java.time.Instant.now().toEpochMilli
              val uniqueId = java.util.UUID.randomUUID().toString

              log.info(s"getWebCamStream[init][$uniqueId]")

              val settings = ConsumerSettings(
                bootstrapServers.split(',').toList
              )
                .withProperties(
                  "sasl.mechanism" -> "SCRAM-SHA-256",
                  "security.protocol" -> "SASL_SSL",
                  "sasl.jaas.config" -> s"""org.apache.kafka.common.security.scram.ScramLoginModule required username="${user}" password="${password}";"""
                )
                .withGroupId(s"group-${user}-${uniqueId}")
                .withClientId(s"client-${user}-${uniqueId}")
                .withOffsetRetrieval(Consumer.OffsetRetrieval.Auto(Consumer.AutoOffsetStrategy.Latest))

              val topic = s"${user}-default"
              val subscription = Subscription.topics(topic)

              val consumerManaged = Consumer.make(settings)
              val consumer = ZLayer.fromManaged(consumerManaged)

              Consumer
                .subscribeAnd(subscription)
                .plainStream(Serde.string, Serde.string)
                .mapM { committableRecord =>
                  log.debug(s"getWebCamStream[received][${uniqueId}][${committableRecord.record.key}][${committableRecord.record.value}]")

                  Task.succeed(committableRecord)
                }
                .filter { committableRecord =>
                  (committableRecord.record.key == webcamKey) &&
                  (committableRecord.record.timestamp() > startTime)
                }
                .map { committableRecord =>
                  val webCamImage = parser.decode[WebCamImage](committableRecord.record.value).toOption

                  log.info(s"getWebCamStream[${uniqueId}][${committableRecord.offset.offset}][$webCamImage]")
                  webCamImage.map(_.dateCreated)
                }
                .provideCustomLayer(consumer)
                .provide(Runtime.default.environment)
            }
          case _ =>
            log.error(s"getWebCamStream[missing_config][$config]")
            Task.succeed(ZStream.empty)
        }
      case Left(ex) =>
        log.error("getWebCamStream[failed_to_load_config]",ex)
        Task.succeed(ZStream.empty)
    }
  }

}

class KafkaServiceModule extends AbstractModule {
  override def configure() = {
    bind(classOf[KafkaService]).to(classOf[KafkaServiceImpl])
  }
}
