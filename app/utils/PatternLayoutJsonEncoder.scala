/*
 * Copyright 2019 HM Revenue & Customs
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

import java.net.InetAddress
import java.nio.charset.StandardCharsets

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.{ILoggingEvent, ThrowableProxyUtil}
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase
import com.fasterxml.jackson.core.JsonGenerator.Feature
import com.fasterxml.jackson.databind.ObjectMapper
import javax.inject.Inject
import org.apache.commons.io.IOUtils._
import org.apache.commons.lang3.time.FastDateFormat
import play.api.{Configuration, Play}

class PatternLayoutJsonEncoder @Inject()(configuration: Configuration) extends PatternLayoutEncoderBase[ILoggingEvent] {

  import scala.collection.JavaConversions._

  private val mapper = new ObjectMapper().configure(Feature.ESCAPE_NON_ASCII, true)

  lazy val appName: String = configuration.get[String]("appName")

  private lazy val dateFormat = FastDateFormat.getInstance(
    configuration.get[String]("logger.json.dateformat")
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

    Option(getContext).map(c =>
      c.getCopyOfPropertyMap.toMap foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }
    )
    event.getMDCPropertyMap.toMap foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }

    s"${mapper.writeValueAsString(eventNode)}$LINE_SEPARATOR".getBytes(StandardCharsets.UTF_8)
  }

  override def footerBytes(): Array[Byte] =
    LINE_SEPARATOR.getBytes(StandardCharsets.UTF_8)

  override def headerBytes(): Array[Byte] =
    LINE_SEPARATOR.getBytes(StandardCharsets.UTF_8)

  override def start() {
    val patternLayout = new PatternLayout()
    patternLayout.setContext(context)
    patternLayout.setPattern(getPattern)
    patternLayout.start()
    this.layout = patternLayout
    super.start()
  }
}
