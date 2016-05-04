/*
 * Copyright 2016 HM Revenue & Customs
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

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.MetricsRegistry

import java.util.concurrent.TimeUnit
import models.ApiType
import models.ApiType.ApiType

trait Metrics {
  def startTimer(api: ApiType): Timer.Context
  def incrementSuccessCounter(api: ApiType.ApiType): Unit
  def incrementTotalCounter(api: ApiType.ApiType): Unit
}

object Metrics extends Metrics {

  val timers = Map(
    ApiType.FindCitizen -> MetricsRegistry.defaultRegistry.timer("find-citizen-response-timer"),
    ApiType.FindRecipient -> MetricsRegistry.defaultRegistry.timer("find-recipient-response-timer"),
    ApiType.CheckRelationship -> MetricsRegistry.defaultRegistry.timer("check-relationship-response-timer"),
    ApiType.CreateRelationship -> MetricsRegistry.defaultRegistry.timer("create-relationship-response-timer"),
    ApiType.ListRelationship -> MetricsRegistry.defaultRegistry.timer("list-relationship-response-timer"),
    ApiType.UpdateRelationship -> MetricsRegistry.defaultRegistry.timer("update-relationship-response-timer"))

  val successCounters = Map(
    ApiType.FindCitizen -> MetricsRegistry.defaultRegistry.counter("find-citizen-success"),
    ApiType.FindRecipient -> MetricsRegistry.defaultRegistry.counter("find-recipient-success"),
    ApiType.CheckRelationship -> MetricsRegistry.defaultRegistry.counter("check-relationship-success"),
    ApiType.CreateRelationship -> MetricsRegistry.defaultRegistry.counter("create-relationship-success"),
    ApiType.ListRelationship -> MetricsRegistry.defaultRegistry.counter("list-relationship-success"),
    ApiType.UpdateRelationship -> MetricsRegistry.defaultRegistry.counter("update-relationship-success"))

  val totalCounters = Map(
    ApiType.FindCitizen -> MetricsRegistry.defaultRegistry.counter("find-citizen-total"),
    ApiType.FindRecipient -> MetricsRegistry.defaultRegistry.counter("find-recipient-total"),
    ApiType.CheckRelationship -> MetricsRegistry.defaultRegistry.counter("check-relationship-total"),
    ApiType.CreateRelationship -> MetricsRegistry.defaultRegistry.counter("create-relationship-total"),
    ApiType.ListRelationship -> MetricsRegistry.defaultRegistry.counter("list-relationship-total"),
    ApiType.UpdateRelationship -> MetricsRegistry.defaultRegistry.counter("update-relationship-total"))

  override def startTimer(api: ApiType): Context = timers(api).time()

  override def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  override def incrementTotalCounter(api: ApiType): Unit = totalCounters(api).inc()
}
