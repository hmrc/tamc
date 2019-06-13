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

package fixtures

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{MetricRegistry, Timer}
import metrics.Metrics
import models.ApiType.ApiType
import org.scalatest.mockito.MockitoSugar

class FakeMetric extends Metrics(MockitoSugar.mock[MetricRegistry]) {

      val fakeTimerContext = MockitoSugar.mock[Timer.Context]

      override def startTimer(api: ApiType): Context = fakeTimerContext

      override def incrementSuccessCounter(api: ApiType): Unit = {}

      override def incrementTotalCounter(api: ApiType): Unit = {}

}
