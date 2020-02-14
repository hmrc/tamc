/*
 * Copyright 2020 HM Revenue & Customs
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

package service

import Fixtures.MultiYearCreateRelationshipRequestHolderFixture
import com.codahale.metrics.Timer
import config.ApplicationConfig.{MA_SUPPORTED_YEARS_COUNT, START_TAX_YEAR}
import connectors.{EmailConnector, MarriageAllowanceDESConnector, MarriageAllowanceDataConnector}
import errors.TooManyRequestsError
import metrics.Metrics
import models.ApiType.ApiType
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.TestData
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.MarriageAllowanceService
import test_utils.TestUtility
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, _}
import uk.gov.hmrc.play.test.UnitSpec
import utils.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

//TODO remove the need for TestUtility
class MarriageAllowanceServiceTest extends UnitSpec with TestUtility with MockitoSugar with GuiceOneAppPerTest {

  implicit override def newAppForTest(testData: TestData): Application = {

    val builder = new GuiceApplicationBuilder()

    testData.name match {
      case testName if testName.contains("post enabled is true") => builder.configure("des.post.enabled" -> true).build()
      case testName if testName.contains("post enabled is false") => builder.configure("des.post.enabled" -> false).build()
      case _ => builder.build()
    }
  }

  val year = 2019
  val generatedNino = new Generator().nextNino

  trait Setup {

    val cID = 123456789
    val findRecipientRequest = FindRecipientRequest(name = "testForename1", lastName = "testLastName",
       gender = Gender("M"), nino = generatedNino, dateOfMarriage = Some(new LocalDate(year,12,12)))
    val userRecord = UserRecord(cid = cID, timestamp = "20200116155359011123")

  }


  lazy val service = new MarriageAllowanceService {
    override val dataConnector = mock[MarriageAllowanceDataConnector]
    override val emailConnector = mock[EmailConnector]
    override val metrics = mock[Metrics]
    override val startTaxYear = START_TAX_YEAR
    override val maSupportedYearsCount = MA_SUPPORTED_YEARS_COUNT

    override def currentTaxYear: Int = year

  }

  "MarriageAllowanceService" should {

    "return a MarriageAllowanceDESConnector if toggle post enabled is true" in {
      MarriageAllowanceService.getConnectorImplementation shouldBe MarriageAllowanceDESConnector
    }

    "return a MarriageAllowanceDataConnector if toggle post enabled is false" in {
      MarriageAllowanceService.getConnectorImplementation shouldBe MarriageAllowanceDataConnector
    }
  }


  "getRecipientRelationship" should {

    "return a UserRecord, list of TaxYearModel tuple given a valid FindRecipientRequest " in  new Setup {

      val taxYearModel = TaxYear(year, Some(true))

      val expectedResponse = (userRecord, List(taxYearModel))

      when(service.dataConnector.findRecipient(ArgumentMatchers.eq(findRecipientRequest))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(userRecord)))

      when(service.dataConnector.findCitizen(ArgumentMatchers.eq(generatedNino))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(findCitizenJson))

      when(service.dataConnector.listRelationship(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(listRelationshipdJson))

      val mockTimerContext = mock[Timer.Context]
      when(service.metrics.startTimer(ArgumentMatchers.any())).thenReturn(mockTimerContext)
      when(mockTimerContext.stop()).thenReturn(123456789)

      val result = await(service.getRecipientRelationship(generatedNino, findRecipientRequest))

      result shouldBe Right(expectedResponse)

    }


    "return a DataRetrievalError error type based on error returned" in new Setup {

        when(service.dataConnector.findRecipient(ArgumentMatchers.eq(findRecipientRequest))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(TooManyRequestsError)))

        val result = await(service.getRecipientRelationship(generatedNino, findRecipientRequest))

        result shouldBe Left(TooManyRequestsError)

    }

  }

  "when request is sent with deceased recipient in MarriageAllowanceService" should {
    "return a BadRequestException" in {
      val service = new FakeDeceasedMarriageAllowanceService
      val multiYearCreateRelationshipRequest = MultiYearCreateRelationshipRequestHolderFixture.multiYearCreateRelationshipRequestHolder
      val response = service.createMultiYearRelationship(multiYearCreateRelationshipRequest, "GDS")(new HeaderCarrier(), implicitly)

      intercept[BadRequestException] {
          await(response)
        }
    }
  }

  val findCitizenJson = Json.parse(s"""

    {"Jtpr1311PerDetailsFindcallResponse": {"Jtpr1311PerDetailsFindExport": {
      "@exitStateType": "3",
      "@exitStateMsg": "",
      "@command": "",
      "@exitState": "0",
      "OutItpr1Person":    {
      "InstanceIdentifier": 100013333,
      "UpdateTimestamp": "2015",
      "Nino": "${generatedNino.nino}",
      "NinoStatus": null,
      "TemporaryReference": "00B00004",
      "Surname": "testSurname",
      "FirstForename": "testForeName",
      "SecondForename": "testSecondForename",
      "Initials": "AF",
      "Title": "MR",
      "Honours": "BSC",
      "Sex": "M",
      "DateOfBirth": 19400610,
      "DeceasedSignal": "N",
      "OrgUnitInstance": 0
    },
      "OutGroupOfIrInterestArea": {"row":    [
    {"OutRgItpr1IrInterestArea":       {
      "Code": 2,
      "Description": "PAY AS YOU EARN",
      "Reference": null,
      "Reason": "FOR TEST PAYE",
      "StartDatetime": 2.0000403E19
    }}]},
      "OutGroupOfCommunications": {"row":    [
    {"OutRgItpr1Communication":       {
      "Code": 7,
      "Description": "DAYTIME TELEPHONE",
      "ContactDetails": "00000 290000"
    }}
      ]},
      "OutGroupOfOccupancy": {"row":    [
    {"OutRgItpr1Occupancy":       {
      "StatusCode": null,
      "StatusDescription": null,
      "TypeOfOccupancyCode": 0,
      "TypeOfOccupancyDescription": null,
      "AdditionalDeliveryInformation": null,
      "RlsSignal": null,
      "TypeOfAddressCode": null,
      "TypeOfAddressDescription": null,
      "AddressLine1": null,
      "AddressLine2": null,
      "AddressLine3": null,
      "AddressLine4": null,
      "AddressLine5": null,
      "Postcode": null,
      "HouseIdentification": null,
      "HomeCountry": null,
      "ForeignCountry": null,
      "NoLongerUsedSignal": null
    }}
      ]},
      "OutItpr1Capacitor": {"InstanceIdentifier": 0},
      "OutWCbdParameters":    {
      "SeverityCode": "I",
      "DataStoreStatus": 1,
      "OriginServid": 1011,
      "ContextString": null,
      "ReturnCode": 1,
      "ReasonCode": 1,
      "Checksum": null
    }
    }}}""")

  val listRelationshipdJson = Json.parse(

    """{
      "relationships": [
      {
        "participant": 1,
        "creationTimestamp": "20150531235901",
        "actualStartDate": "20011230",
        "relationshipEndReason": "Death",
        "participant1StartDate": "20011230",
        "participant2StartDate": "20011230",
        "actualEndDate": "20101230",
        "participant1EndDate": "20101230",
        "participant2EndDate": "20101230",
        "otherParticipantInstanceIdentifier": "123456789012345",
        "otherParticipantUpdateTimestamp": "20150531235901",
        "participant2UKResident": true
      },
      {
        "participant": 1,
        "creationTimestamp": "20150531235901",
        "actualStartDate": "20011230",
        "relationshipEndReason": "Death",
        "participant1StartDate": "20011230",
        "participant2StartDate": "20011230",
        "actualEndDate": "20101230",
        "participant1EndDate": "20101230",
        "participant2EndDate": "20101230",
        "otherParticipantInstanceIdentifier": "123456789012345",
        "otherParticipantUpdateTimestamp": "20150531235901",
        "participant2UKResident": true
      }
      ]
    }""")

}







class FakeDeceasedMarriageAllowanceService extends MarriageAllowanceService {
  override val dataConnector = mockDeceasedDataConnector
  override val emailConnector = mockEmailConnector
  override val metrics = Metrics
  override val startTaxYear = 2015
  override val maSupportedYearsCount = 5

}

class FakeAuthorityMarriageAllowanceService extends MarriageAllowanceService {
  override val dataConnector = mockAuthorityDataConnector
  override val emailConnector = mockEmailConnector
  override val metrics = Metrics
  override val startTaxYear = 2015
  override val maSupportedYearsCount = 5

}

object mockDeceasedDataConnector extends MarriageAllowanceDataConnector {
  override val httpGet = WSHttp
  override val httpPost = WSHttp
  override val httpPut = WSHttp
  override val serviceUrl = ""
  override val urlHeaderEnvironment = ""
  override val urlHeaderAuthorization = "foo"
  override val metrics = Metrics

  override def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)
                                                     (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    Future.failed(new BadRequestException("{\"reason\": \"Participant is deceased\"}"))
  }
}

object mockAuthorityDataConnector extends MarriageAllowanceDataConnector {
  override val httpGet = WSHttp
  override val httpPost = WSHttp
  override val httpPut = WSHttp
  override val serviceUrl = ""
  override val urlHeaderEnvironment = ""
  override val urlHeaderAuthorization = "foo"
  override val metrics = Metrics

  override def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)
                                                     (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    Future.failed(new BadRequestException("{\"reason\": \"User does not have authority to retrieve requested Participant 1 record\"}"))
  }
}

object mockEmailConnector extends EmailConnector {
  override val httpPost = WSHttp
  override val emailUrl = "bar"
  override def sendEmail(sendEmailRequest: SendEmailRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    Future(HttpResponse(200,None))
  }
}

object Metrics extends Metrics {
  def startTimer(api: ApiType): Timer.Context = (new Timer).time()

  def incrementSuccessCounter(api: ApiType.ApiType) = {}

  def incrementTotalCounter(api: ApiType.ApiType) = {}

  def incrementFailedCounter(api: ApiType) = {}
}
