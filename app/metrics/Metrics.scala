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
import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import javax.inject.Inject
import models.ApiType
import models.ApiType.ApiType

class Metrics @Inject()(metrics: MetricRegistry) {

  val timers: Map[models.ApiType.Value, Timer] = Map(
    ApiType.FindCitizen -> metrics.timer("find-citizen-response-timer"),
    ApiType.FindRecipient -> metrics.timer("find-recipient-response-timer"),
    ApiType.CheckRelationship -> metrics.timer("check-relationship-response-timer"),
    ApiType.CreateRelationship -> metrics.timer("create-relationship-response-timer"),
    ApiType.ListRelationship -> metrics.timer("list-relationship-response-timer"),
    ApiType.UpdateRelationship -> metrics.timer("update-relationship-response-timer"))

  val successCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.FindCitizen -> metrics.counter("find-citizen-success"),
    ApiType.FindRecipient -> metrics.counter("find-recipient-success"),
    ApiType.CheckRelationship -> metrics.counter("check-relationship-success"),
    ApiType.CreateRelationship -> metrics.counter("create-relationship-success"),
    ApiType.ListRelationship -> metrics.counter("list-relationship-success"),
    ApiType.UpdateRelationship -> metrics.counter("update-relationship-success"))

  val totalCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.FindCitizen -> metrics.counter("find-citizen-total"),
    ApiType.FindRecipient -> metrics.counter("find-recipient-total"),
    ApiType.CheckRelationship -> metrics.counter("check-relationship-total"),
    ApiType.CreateRelationship -> metrics.counter("create-relationship-total"),
    ApiType.ListRelationship -> metrics.counter("list-relationship-total"),
    ApiType.UpdateRelationship -> metrics.counter("update-relationship-total"))

  def startTimer(api: ApiType): Context = timers(api).time()

  def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  def incrementTotalCounter(api: ApiType): Unit = totalCounters(api).inc()
}
