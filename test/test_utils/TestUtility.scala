/*
 * Copyright 2017 HM Revenue & Customs
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
import com.kenshoo.play.metrics.PlayModule
import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import controllers.MarriageAllowanceController
import errors.ErrorResponseStatus._
import metrics.Metrics
import models.ApiType.ApiType
import models._
import org.joda.time._
import org.scalatest.mock.MockitoSugar
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{JsValue, Writes}
import play.api.{Application, Mode}
import services.MarriageAllowanceService
import test_utils.TestData.{Cids, findMockData}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost, WSPut}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http._
import _root_.controllers.MarriageAllowanceController.JSON
import play.api.http.Status
import play.api.mvc.Results

trait TestUtility {

  def bindModules: Seq[GuiceableModule] = Seq(new PlayModule)

  implicit lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .bindings(bindModules: _*)
    .in(Mode.Test)
    .build()

  def makeFakeController(testingTime: DateTime = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.forID("Europe/London")), isErrorController: Boolean = false) = {

    val fakeHttpGet = new HttpGet with WSGet{
      override val hooks = NoneRequired
      var httpGetCallsToTest: List[HttpGETCallWithHeaders] = List()

      override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
        val adjustedUrl = s"GET-${url}"
        httpGetCallsToTest = httpGetCallsToTest :+ HttpGETCallWithHeaders(url = adjustedUrl, env = hc.extraHeaders, bearerToken = hc.authorization)
        var responseBody = findMockData(adjustedUrl)
        val response = new DummyHttpResponse(responseBody, 200)
        return Future.successful(response)
      }
    }

    val fakeHttpPost = new HttpPost with WSPost {
      override val hooks = NoneRequired
      var httpPostCallsToTest: List[HttpPOSTCallWithHeaders] = List()

      override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        val adjustedUrl = s"POST-${url}"
        httpPostCallsToTest = httpPostCallsToTest :+ HttpPOSTCallWithHeaders(url = adjustedUrl, body = body, env = hc.extraHeaders, bearerToken = hc.authorization)
        var responseBody = findMockData(adjustedUrl, Some(body))
        val response = new DummyHttpResponse(responseBody, 200)
        Future.successful(response)
      }

      override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    }

    val fakeHttpPut = new HttpPut with WSPut {
      override val hooks = NoneRequired
      var httpPutCallsToTest: List[HttpPUTCallWithHeaders] = List()

      override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[_root_.uk.gov.hmrc.http.HttpResponse] = {
        val adjustedUrl = s"PUT-${url}"
        val adjustedBody = body.asInstanceOf[DesUpdateRelationshipRequest]
        httpPutCallsToTest = httpPutCallsToTest :+ HttpPUTCallWithHeaders(url = adjustedUrl, body = adjustedBody, env = hc.extraHeaders, bearerToken = hc.authorization)
        var responseBody = findMockData(adjustedUrl, Some(adjustedBody))
        val response = new DummyHttpResponse(responseBody, 200)
        Future.successful(response)
      }

      protected def doPutString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      protected def doEmptyPut[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      protected def doFormPut(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    }

    val fakeHttpEmailPost = new HttpPost with WSPost {
      override val hooks = NoneRequired

      var checkEmailCallCount = 0
      var checkEmailCallData: Option[SendEmailRequest] = None

      override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        checkEmailCallCount = checkEmailCallCount + 1
        checkEmailCallData = Some(body.asInstanceOf[SendEmailRequest])
        body.asInstanceOf[SendEmailRequest] match {
          case SendEmailRequest(addressList, _, _, _) if (addressList.exists { address => address.value.equals("bad-request@example.com") }) =>
            Future.failed(new BadRequestException("throwing error for:" + addressList))
          case _ =>
            Future.successful(new DummyHttpResponse("", 200))
        }
      }

      override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    }

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
        def errorResponse(reason: String, code: Int) = Future {
          new DummyHttpResponse(reason, code)
        }

        isErrorController match {
          case true =>
            data.participant1.instanceIdentifier.toLong match {
              case Cids.cidBadRequest => errorResponse("""{"Reason":"Cannot update as Participant 1 update time stamp has changed since last view of data"}""", 400)
              case Cids.cidCitizenNotFound => errorResponse("""{"Reason":"Person Instance identifier not found"}""", 404)
              case _ => throw new Exception("this exception should not be thrown")
            }
          case false => updateAllowanceRelationshipDataToTest = Some(data)
            updateAllowanceRelationshipDataToTestCount = updateAllowanceRelationshipDataToTestCount + 1
            super.updateAllowanceRelationship(data)
        }
      }

      override def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit ec: ExecutionContext): Future[JsValue] = {
        isErrorController match {
          case true =>
            cid match {
              case Cids.cidBadRequest => throw new BadRequestException(BAD_REQUEST)
              case Cids.cidCitizenNotFound => throw new NotFoundException(CITIZEN_NOT_FOUND)
              case Cids.cidServerError => throw new InternalServerException(SERVER_ERROR)
              case Cids.cidServiceUnavailable => throw new ServiceUnavailableException(SERVICE_UNAVILABLE)
              case _ => throw new Exception("this exception should not be thrown")
            }
          case _ => super.listRelationship(cid, includeHistoric)
        }
      }
    }

    val fakeMetrics = new Metrics {
      val fakeTimerContext = MockitoSugar.mock[Timer.Context]

      override def startTimer(api: ApiType): Context = fakeTimerContext

      override def incrementSuccessCounter(api: ApiType): Unit = {}

      override def incrementTotalCounter(api: ApiType): Unit = {}
    }

    val fakeTaxYearResolver = new TaxYearResolver {
      override lazy val now = () => testingTime
    }

    val fakeMarriageAllowanceService = new MarriageAllowanceService {
      override val dataConnector = fakeMarriageAllowanceDataConnector
      override val emailConnector = fakeEmailConnector
      override val metrics = fakeMetrics
      override val taxYearResolver = fakeTaxYearResolver
      override val startTaxYear = 2015
      override val maSupportedYearsCount = 5

      override def createMultiYearRelationship(createRelationshipRequestHolder: MultiYearCreateRelationshipRequestHolder, journey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
        isErrorController match {
          case true =>
            createRelationshipRequestHolder.request.transferor_cid match {
              case Cids.cidBadRequest => throw new BadRequestException(BAD_REQUEST)
              case Cids.cidConflict => Future.failed(new Upstream4xxResponse("Cannot update as Participant", 409, 409))
              case Cids.cidServiceUnavailable => Future.failed(new Upstream5xxResponse("LTM000503", 503, 503))
              case _ => throw new Exception("this exception should not be thrown")
            }
          case _ => super.createMultiYearRelationship(createRelationshipRequestHolder, journey)
        }
      }
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
