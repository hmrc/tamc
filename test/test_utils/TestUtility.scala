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

package test_utils

import akka.actor.ActorSystem
import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{MetricRegistry, Timer}
import com.kenshoo.play.metrics.PlayModule
import com.typesafe.config.Config
import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import errors.ErrorResponseStatus._
import javax.inject.Inject
import metrics.Metrics
import models.ApiType.ApiType
import models._
import org.joda.time._
import org.scalatest.mock.MockitoSugar
import play.api.Play
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsValue, Writes}
import services.MarriageAllowanceService
import test_utils.TestData.{Cids, findMockData}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.{WSPost, WSPut}
import uk.gov.hmrc.time

import scala.concurrent.{ExecutionContext, Future}

class TestUtility @Inject()(httpClient: HttpClient,
                            dataConnector: MarriageAllowanceDataConnector,
                            emailConnector: EmailConnector,
                            metrics: Metrics,
                            metricRegistry: MetricRegistry) {

  def bindModules: Seq[GuiceableModule] = Seq(new PlayModule)






    val debugObject = new Object {
      def findCitizenNinoToTest = fakeMarriageAllowanceDataConnector.findCitizenNinoToTest

      def findRecipientNinoToTest = fakeMarriageAllowanceDataConnector.findRecipientNinoToTest

      def findCitizenNinoToTestCount = fakeMarriageAllowanceDataConnector.findCitizenNinoToTestCount

      def findRecipientNinoToTestCount = fakeMarriageAllowanceDataConnector.findRecipientNinoToTestCount

      def checkAllowanceRelationshipCidToTest = fakeMarriageAllowanceDataConnector.checkAllowanceRelationshipCidToTest

      def checkAllowanceRelationshipCidToTestCount = fakeMarriageAllowanceDataConnector.checkAllowanceRelationshipCidToTestCount

      def createAllowanceRelationshipDataToTest = fakeMarriageAllowanceDataConnector.createAllowanceRelationshipDataToTest

      def createAllowanceRelationshipDataToTestCount = fakeMarriageAllowanceDataConnector.createAllowanceRelationshipDataToTestCount

      def updateAllowanceRelationshipDataToTest = fakeMarriageAllowanceDataConnector.updateAllowanceRelationshipDataToTest

      def updateAllowanceRelationshipDataToTestCount = fakeMarriageAllowanceDataConnector.updateAllowanceRelationshipDataToTestCount

      def httpGetCallsToTest = fakeHttpGet.httpGetCallsToTest

      def httpPostCallsToTest = fakeHttpPost.httpPostCallsToTest

      def checkEmailCallCount = fakeHttpEmailPost.checkEmailCallCount

      def checkEmailCallData = fakeHttpEmailPost.checkEmailCallData

      def httpPutCallsToTest = fakeHttpPut.httpPutCallsToTest
    }

    new MarriageAllowanceController {
      override val marriageAllowanceService = fakeMarriageAllowanceService
      val debugData = debugObject
    }

}
