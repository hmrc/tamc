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

package utils

import config.ApplicationConfig
import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import controllers.MarriageAllowanceController
import fixtures.{FakeHttpClient, FakeMarriageAllowanceDataConnector, FakeMarriageAllowanceErrorControllerDataConnector, FakeMarriageAllowanceErrorControllerService, FakeMarriageAllowanceService, FakeMetric, MockEmailConnector}
import javax.inject.Inject
import metrics.Metrics
import org.joda.time._
import play.api.{Configuration, Environment}
import services.MarriageAllowanceService
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext

class TestUtility @Inject()(applicationConfig: ApplicationConfig, environment: Environment, runModeConfiguration: Configuration){

  def makeFakeController(testingTime: DateTime = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.forID("Europe/London")),
                         isErrorController: Boolean = false)(implicit ec: ExecutionContext): MarriageAllowanceController = {

    val httpClient: HttpClient = new FakeHttpClient

    val emailConnector: EmailConnector = new MockEmailConnector(httpClient, environment, runModeConfiguration)
    val metrics: Metrics = new FakeMetric

    val dataConnector: MarriageAllowanceDataConnector = if(isErrorController){
      new FakeMarriageAllowanceDataConnector(httpClient, environment, runModeConfiguration)
    } else {
      new FakeMarriageAllowanceErrorControllerDataConnector(httpClient, environment, runModeConfiguration)
    }

    val marriageAllowanceService: MarriageAllowanceService = if(isErrorController){
      new FakeMarriageAllowanceService(applicationConfig, dataConnector, emailConnector, metrics)
    } else {
      new FakeMarriageAllowanceErrorControllerService(applicationConfig, dataConnector, emailConnector, metrics)
    }

    new MarriageAllowanceController(marriageAllowanceService)
  }

}
