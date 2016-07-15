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

package test_utils

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import controllers.MarriageAllowanceController
import metrics.Metrics
import models.ApiType.ApiType
import models.{Cid, DesCreateRelationshipRequest, DesUpdateRelationshipRequest, FindRecipientRequest}
import org.joda.time._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import services.MarriageAllowanceService
import test_utils.FakeHttpVerbs._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpPut, HttpResponse}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.{ExecutionContext, Future}

class DummyHttpResponse(override val body: String, override val status: Int, override val allHeaders: Map[String, Seq[String]] = Map.empty) extends HttpResponse {
  override def json: JsValue = Json.parse(body)
}

trait TestUtility {


  def makeFakeErrorController() = {
    makeFakeController()
  }

  def makeFakeController(testingTime: DateTime = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.forID("Europe/London"))) = {

    val fakeEmailConnector = new EmailConnector {
      override val httpPost = fakeHttpEmailPost
      override val emailUrl = "email-url"
    }

    val fakeMarriageAllowanceDataConnector = new MarriageAllowanceDataConnector {
      override val httpGet: HttpGet = fakeHttpGet
      override val httpPost: HttpPost = fakeHttpPost
      override val httpPut: HttpPut = fakeHttpPut
      override val serviceUrl: String = "foo"
      override val urlHeaderEnvironment = "test-environment"
      override val urlHeaderAuthorization = "test-bearer-token"

      var findCitizenNinoToTest: Option[Nino] = None
      var findRecipientNinoToTest: Option[Nino] = None
      var findCitizenNinoToTestCount = 0
      var findRecipientNinoToTestCount = 0
      var checkAllowanceRelationshipCidToTest: Option[Cid] = None
      var checkAllowanceRelationshipCidToTestCount = 0
      var createAllowanceRelationshipDataToTest: Option[DesCreateRelationshipRequest] = None
      var createAllowanceRelationshipDataToTestCount = 0
      var updateAllowanceRelationshipDataToTest: Option[DesUpdateRelationshipRequest] = None
      var updateAllowanceRelationshipDataToTestCount = 0

      override def findCitizen(nino: Nino)(implicit ec: ExecutionContext): Future[JsValue] = {
        findCitizenNinoToTest = Some(nino)
        findCitizenNinoToTestCount = findCitizenNinoToTestCount + 1
        super.findCitizen(nino)
      }

      override def findRecipient(findRecipientRequest: FindRecipientRequest)(implicit ec: ExecutionContext): Future[JsValue] = {
        findRecipientNinoToTest = Some(findRecipientRequest.nino)
        findRecipientNinoToTestCount = findRecipientNinoToTestCount + 1
        super.findRecipient(findRecipientRequest)
      }

      override def updateAllowanceRelationship(data: DesUpdateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
        updateAllowanceRelationshipDataToTest = Some(data)
        updateAllowanceRelationshipDataToTestCount = updateAllowanceRelationshipDataToTestCount + 1
        super.updateAllowanceRelationship(data)
      }
    }

    val fakeMetrics = new Metrics {
      val fakeTimerContext = MockitoSugar.mock[Timer.Context]
      override def startTimer(api: ApiType): Context = fakeTimerContext
      override def incrementSuccessCounter(api: ApiType): Unit = {}
      override def incrementTotalCounter(api: ApiType): Unit = {}
    }

    val fakeTaxYearResolver = new TaxYearResolver{
      override lazy val now = () => testingTime
    }

    val fakeMarriageAllowanceService = new MarriageAllowanceService {
      override val dataConnector = fakeMarriageAllowanceDataConnector
      override val emailConnector = fakeEmailConnector
      override val metrics = fakeMetrics
      override val taxYearResolver =fakeTaxYearResolver
      override val startTaxYear = 2015
    }

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
}

