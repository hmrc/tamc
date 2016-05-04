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

import java.net.URLDecoder
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.io.Source
import org.scalatest.mock.MockitoSugar
import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import connectors.EmailConnector
import connectors.MarriageAllowanceDataConnector
import controllers.MarriageAllowanceController
import metrics.Metrics
import models.ApiType.ApiType
import models.Cid
import models.DesCreateRelationshipRequest
import models.DesUpdateRelationshipRequest
import models.FindRecipientRequest
import models.ResponseStatus
import models.SendEmailRequest
import models.UserRecord
import play.api.libs.json.Format
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Writes
import services.MarriageAllowanceService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.BadRequestException
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.HttpPost
import uk.gov.hmrc.play.http.HttpPut
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.http.logging.Authorization
import models.DesCreateRelationshipRequest
import models.MultiYearDesCreateRelationshipRequest
import org.joda.time._

//FIXME should we take DummyHttpResponse from http-verbs test.jar?
class DummyHttpResponse(override val body: String, override val status: Int, override val allHeaders: Map[String, Seq[String]] = Map.empty) extends HttpResponse {
  override def json: JsValue = Json.parse(body)
}

case class HttpGETCallWithHeaders(url: String, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)
case class HttpPOSTCallWithHeaders(url: String, body: Any, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)
case class HttpPUTCallWithHeaders(url: String, body: DesUpdateRelationshipRequest, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)

trait TestUtility {

  def makeFakeController(testingTime: DateTime = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.forID("Europe/London"))) = {

    val fakeHttpGet = new HttpGet {
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

    val fakeHttpPost = new HttpPost {
      override val hooks = NoneRequired
      var httpPostCallsToTest: List[HttpPOSTCallWithHeaders] = List()
      protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        val adjustedUrl = s"POST-${url}"
        httpPostCallsToTest = httpPostCallsToTest :+ HttpPOSTCallWithHeaders(url = adjustedUrl, body = body, env = hc.extraHeaders, bearerToken = hc.authorization)
        var responseBody = findMockData(adjustedUrl, Some(body))
        val response = new DummyHttpResponse(responseBody, 200)
        Future.successful(response)
      }
      protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
      protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
      protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    }

    val fakeHttpPut = new HttpPut {
      override val hooks = NoneRequired
      var httpPutCallsToTest: List[HttpPUTCallWithHeaders] = List()
      protected def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[uk.gov.hmrc.play.http.HttpResponse] = {
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

    val fakeHttpEmailPost = new HttpPost {
      override val hooks = NoneRequired

      var checkEmailCallCount = 0
      var checkEmailCallData: Option[SendEmailRequest] = None

      protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        checkEmailCallCount = checkEmailCallCount + 1
        checkEmailCallData = Some(body.asInstanceOf[SendEmailRequest])
        body.asInstanceOf[SendEmailRequest] match {
          case SendEmailRequest(addressList, _, _, _) if (addressList.exists { address => address.value.equals("bad-request@example.com") }) =>
            Future.failed(new BadRequestException("throwing error for:" + addressList))
          case _ =>
            Future.successful(new DummyHttpResponse("", 200))
        }
      }
      protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
      protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
      protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
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

  def decodeQueryStringValue(value: String) =
    URLDecoder.decode(value, "UTF-8")

  def findMockData[T](url: String, body: Option[T] = None): String = {

    val findCitizenByNinoUrl = """^GET-foo/marriage-allowance/citizen/((?!BG|GB|NK|KN|TN|NT|ZZ)[ABCEGHJ-PRSTW-Z][ABCEGHJ-NPRSTW-Z]\d{6}[A-D]$)""".r
    val findRecipientByNinoUrl = """^GET-foo/marriage-allowance/citizen/((?!BG|GB|NK|KN|TN|NT|ZZ)[ABCEGHJ-PRSTW-Z][ABCEGHJ-NPRSTW-Z]\d{6}[A-D])/check\?surname=(.*)\&forename1=(.*)\&gender=(.*)""".r
    val checkAllowanceRelationshipUrl = """^GET-foo/marriage-allowance/citizen/(\d+)/relationship""".r
    val listRelationshipUrl = """^GET-foo/marriage-allowance/citizen/(\d+)/relationships\?includeHistoric=true""".r
    val createAllowanceRelationshipUrl = """^POST-foo/marriage-allowance/citizen/(\d+)/relationship""".r
    val multiYearCreateAllowanceRelationshipUrl = """^POST-foo/marriage-allowance/02.00.00/citizen/(\d+)/relationship/([a-zA-Z]+)""".r
    val updateAllowanceRelationshipUrl = """^PUT-foo/marriage-allowance/citizen/(\d+)/relationship""".r

    (url, body) match {
      case (checkAllowanceRelationshipUrl(cid), None) =>
        TestData.mappedCid2CheckAllowanceRelationship(cid).json
      case (createAllowanceRelationshipUrl(recipientCid), Some(body: DesCreateRelationshipRequest)) =>
        val bodyToText = s"trcid-${body.CID2}_trts-${body.CID2Timestamp}_rccid-${body.CID1}_rcts-${body.CID1Timestamp}"
        TestData.mappedCreations(bodyToText).json
      case (multiYearCreateAllowanceRelationshipUrl(recipientCid, reqType: String), Some(body: MultiYearDesCreateRelationshipRequest)) =>
        val bodyToText = s"trcid-${body.transferorCid}_trts-${body.transferorTimestamp}_rccid-${body.recipientCid}_rcts-${body.recipientTimestamp}"
        TestData.mappedMultiYearCreate(bodyToText).json
      case (findCitizenByNinoUrl(nino), None) =>
        TestData.mappedNino2FindCitizen(nino).json
      case (findRecipientByNinoUrl(nino, surname, forename1, gender), None) =>
        val filePath = s"/data/findRecipient/nino-${nino}_surname-${decodeQueryStringValue(surname)}_forename1-${decodeQueryStringValue(forename1)}_gender-${decodeQueryStringValue(gender)}.json"
        TestData.mappedFindRecipient(filePath).json
      case (listRelationshipUrl(cid), None) =>
        val filePath = s"usercid-${cid}"
        TestData.mappedLists(filePath).json
      case (updateAllowanceRelationshipUrl(recipientCid), Some(body: DesUpdateRelationshipRequest)) =>
        val bodyToText = s"cid1-${body.participant1.instanceIdentifier}_part2ts-${body.participant2.updateTimestamp}_endReason-${body.relationship.relationshipEndReason}"
        TestData.mappedUpdates(bodyToText).json
      case _ =>
        throw new IllegalArgumentException("url not supported:" + url)
    }
  }
}

case class TestRelationshipRecordWrapper(relationshipRecordList: Seq[TestRelationshipRecord], userRecord: UserRecord)
case class TestRelationshipRecord(participant: String, creationTimestamp: String, participant1StartDate: String, relationshipEndReason: Option[String] = None, participant1EndDate: Option[String] = None, otherParticipantInstanceIdentifier: String, otherParticipantUpdateTimestamp: String)
case class TestRelationshipRecordStatusWrapper(relationship_record: TestRelationshipRecordWrapper = null, status: ResponseStatus)

object TestRelationshipRecord {

  implicit object TestRelationshipRecordFormat extends Format[TestRelationshipRecord] {
    def reads(json: JsValue) = JsSuccess(new TestRelationshipRecord(

      (json \ "participant").as[String],
      (json \ "creationTimestamp").as[String],
      (json \ "participant1StartDate").as[String],
      (json \ "relationshipEndReason").as[Option[String]],
      (json \ "participant1EndDate").as[Option[String]],
      (json \ "otherParticipantInstanceIdentifier").as[String],
      (json \ "otherParticipantUpdateTimestamp").as[String]))

    def writes(relatisoshipRecord: TestRelationshipRecord) = Json.obj(

      "participant" -> relatisoshipRecord.participant,
      "creationTimestamp" -> relatisoshipRecord.creationTimestamp,
      "participant1StartDate" -> relatisoshipRecord.participant1StartDate,
      "relationshipEndReason" -> relatisoshipRecord.relationshipEndReason,
      "participant1EndDate" -> relatisoshipRecord.participant1EndDate,
      "otherParticipantInstanceIdentifier" -> relatisoshipRecord.otherParticipantInstanceIdentifier,
      "otherParticipantUpdateTimestamp" -> relatisoshipRecord.otherParticipantUpdateTimestamp)
  }
}

object TestRelationshipRecordWrapper {

  implicit object RelationshipRecordListFormat extends Format[TestRelationshipRecordWrapper] {
    def reads(json: JsValue) = JsSuccess(TestRelationshipRecordWrapper(
      (json \ "relationships").as[Seq[TestRelationshipRecord]],
      (json \ "userRecord").as[UserRecord]))
    def writes(relatisoshipRecordWrapper: TestRelationshipRecordWrapper) = Json.obj("relationships" -> relatisoshipRecordWrapper.relationshipRecordList,
      "userRecord" -> relatisoshipRecordWrapper.userRecord)
  }
}

object TestRelationshipRecordStatusWrapper {
  implicit val formats = Json.format[TestRelationshipRecordStatusWrapper]
}
