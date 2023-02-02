/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.logging.tamc

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.{ILoggingEvent, ThrowableProxyUtil}
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase
import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.apache.commons.lang3.time.FastDateFormat
import play.api.Configuration

import java.net.InetAddress
import java.nio.charset.StandardCharsets


class PatternLayoutJsonEncoder @Inject()(configuration: Configuration) extends PatternLayoutEncoderBase[ILoggingEvent] {

  import scala.jdk.CollectionConverters._

  private val mapper = new ObjectMapper().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true)

  lazy val appName: String = configuration.getOptional[String]("appName").getOrElse("APP NAME NOT SET")

  private lazy val dateFormat = FastDateFormat.getInstance(
    configuration.getOptional[String]("logger.json.dateformat").getOrElse("yyyy-MM-dd HH:mm:ss.SSSZZ")
  )

  override def encode(event: ILoggingEvent):Array[Byte] = {
    val eventNode = mapper.createObjectNode

    eventNode.put("app", appName)
    eventNode.put("hostname", InetAddress.getLocalHost.getHostName)
    eventNode.put("timestamp", dateFormat.format(event.getTimeStamp))
    eventNode.put("message", layout.doLayout(event))

    Option(event.getThrowableProxy).map(p =>
      eventNode.put("exception", ThrowableProxyUtil.asString(p))
    )

    eventNode.put("logger", event.getLoggerName)
    eventNode.put("thread", event.getThreadName)
    eventNode.put("level", event.getLevel.toString)

    Option(getContext).foreach(c =>
      c.getCopyOfPropertyMap.asScala.toMap foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }
    )
    event.getMDCPropertyMap.asScala.toMap foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }

    s"${mapper.writeValueAsString(eventNode)}${System.lineSeparator}".getBytes(StandardCharsets.UTF_8)
  }

  override def footerBytes(): Array[Byte] =
    System.lineSeparator.getBytes(StandardCharsets.UTF_8)

  override def headerBytes(): Array[Byte] =
    System.lineSeparator.getBytes(StandardCharsets.UTF_8)

  override def start(): Unit = {
    val patternLayout = new PatternLayout()
    patternLayout.setContext(context)
    patternLayout.setPattern(getPattern)
    patternLayout.start()
    this.layout = patternLayout
    super.start()
  }
}
