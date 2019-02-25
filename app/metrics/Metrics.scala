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

package metrics

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{MetricRegistry, Timer}
import models.ApiType
import models.ApiType.ApiType
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

trait Metrics {
  def startTimer(api: ApiType): Timer.Context

  def incrementSuccessCounter(api: ApiType.ApiType): Unit

  def incrementTotalCounter(api: ApiType.ApiType): Unit
}

object Metrics extends Metrics with MicroserviceMetrics {

  val registry: MetricRegistry = metrics.defaultRegistry

  val timers = Map(
    ApiType.FindCitizen -> registry.timer("find-citizen-response-timer"),
    ApiType.FindRecipient -> registry.timer("find-recipient-response-timer"),
    ApiType.CheckRelationship -> registry.timer("check-relationship-response-timer"),
    ApiType.CreateRelationship -> registry.timer("create-relationship-response-timer"),
    ApiType.ListRelationship -> registry.timer("list-relationship-response-timer"),
    ApiType.UpdateRelationship -> registry.timer("update-relationship-response-timer"))

  val successCounters = Map(
    ApiType.FindCitizen -> registry.counter("find-citizen-success"),
    ApiType.FindRecipient -> registry.counter("find-recipient-success"),
    ApiType.CheckRelationship -> registry.counter("check-relationship-success"),
    ApiType.CreateRelationship -> registry.counter("create-relationship-success"),
    ApiType.ListRelationship -> registry.counter("list-relationship-success"),
    ApiType.UpdateRelationship -> registry.counter("update-relationship-success"))

  val totalCounters = Map(
    ApiType.FindCitizen -> registry.counter("find-citizen-total"),
    ApiType.FindRecipient -> registry.counter("find-recipient-total"),
    ApiType.CheckRelationship -> registry.counter("check-relationship-total"),
    ApiType.CreateRelationship -> registry.counter("create-relationship-total"),
    ApiType.ListRelationship -> registry.counter("list-relationship-total"),
    ApiType.UpdateRelationship -> registry.counter("update-relationship-total"))

  override def startTimer(api: ApiType): Context = timers(api).time()

  override def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  override def incrementTotalCounter(api: ApiType): Unit = totalCounters(api).inc()
}
