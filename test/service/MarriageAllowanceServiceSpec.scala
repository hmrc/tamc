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
import connectors.{EmailConnector, MarriageAllowanceDESConnector}
import errors.TooManyRequestsError
import metrics.TamcMetrics
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.MarriageAllowanceService
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.test.UnitSpec
import play.api.inject.bind

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MarriageAllowanceServiceSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  val year = 2020
  val generatedNino = new Generator().nextNino

  trait Setup {

    val cID = 123456789
    val findRecipientRequest = FindRecipientRequest(name = "testForename1", lastName = "testLastName",
       gender = Gender("M"), nino = generatedNino, dateOfMarriage = Some(new LocalDate(year,12,12)))
    val userRecord = UserRecord(cid = cID, timestamp = "20200116155359011123")

  }

  val mockMarriageAllowanceDESConnector: MarriageAllowanceDESConnector = mock[MarriageAllowanceDESConnector]
  val mockTamcMetrics: TamcMetrics = mock[TamcMetrics]
  val mockEmailConnector: EmailConnector = mock[EmailConnector]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MarriageAllowanceDESConnector].toInstance(mockMarriageAllowanceDESConnector),
      bind[TamcMetrics].toInstance(mockTamcMetrics),
      bind[EmailConnector].toInstance(mockEmailConnector)
    ).build()

  def service = app.injector.instanceOf[MarriageAllowanceService]



//  lazy val service = new MarriageAllowanceService {
//    override val dataConnector = mock[MarriageAllowanceDESConnector]
//    override val emailConnector = mock[EmailConnector]
//    override val metrics = mock[TamcMetrics]
//    override val startTaxYear = 2015
//    override val maSupportedYearsCount = 5
//
//    override def currentTaxYear: Int = year
//
//  }

  "getRecipientRelationship" should {

    "return a UserRecord, list of TaxYearModel tuple given a valid FindRecipientRequest " in  new Setup {

      val taxYearModel = TaxYear(year, Some(true))

      val expectedResponse = (userRecord, List(taxYearModel))

      when(mockMarriageAllowanceDESConnector.findRecipient(ArgumentMatchers.eq(findRecipientRequest))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(userRecord)))

      when(mockMarriageAllowanceDESConnector.findCitizen(ArgumentMatchers.eq(generatedNino))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(findCitizenJson))

      when(mockMarriageAllowanceDESConnector.listRelationship(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(listRelationshipdJson))

      val mockTimerContext = mock[Timer.Context]
      when(mockTamcMetrics.startTimer(ArgumentMatchers.any())).thenReturn(mockTimerContext)
      when(mockTimerContext.stop()).thenReturn(123456789)

      val result = await(service.getRecipientRelationship(generatedNino, findRecipientRequest))

      result shouldBe Right(expectedResponse)

    }


    "return a DataRetrievalError error type based on error returned" in new Setup {

        when(mockMarriageAllowanceDESConnector.findRecipient(ArgumentMatchers.eq(findRecipientRequest))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(TooManyRequestsError)))

        val result = await(service.getRecipientRelationship(generatedNino, findRecipientRequest))

        result shouldBe Left(TooManyRequestsError)

    }

  }

  "when request is sent with deceased recipient in MarriageAllowanceService" should {
    "return a BadRequestException" in {
      when(mockMarriageAllowanceDESConnector.sendMultiYearCreateRelationshipRequest(any(),any())(any(),any())).thenReturn(Future.failed(new BadRequestException("{\"reason\": \"Participant is deceased\"}")))
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


//class FakeDeceasedMarriageAllowanceService extends MarriageAllowanceService {
//  override val dataConnector = mockDeceasedDataConnector
//  override val emailConnector = mockEmailConnector
//  override val metrics = Metrics
//  override val startTaxYear = 2015
//  override val maSupportedYearsCount = 5
//
//}
//
//class FakeAuthorityMarriageAllowanceService extends MarriageAllowanceService {
//  override val dataConnector = mockAuthorityDataConnector
//  override val emailConnector = mockEmailConnector
//  override val metrics = Metrics
//  override val startTaxYear = 2015
//  override val maSupportedYearsCount = 5
//
//}
//
//object mockDeceasedDataConnector extends MarriageAllowanceDESConnector {
//  override val httpGet = WSHttp
//  override val httpPost = WSHttp
//  override val httpPut = WSHttp
//  override val serviceUrl = ""
//  override val urlHeaderEnvironment = ""
//  override val urlHeaderAuthorization = "foo"
//  override val metrics = Metrics
//
//  override def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)
//                                                     (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
//    Future.failed(new BadRequestException("{\"reason\": \"Participant is deceased\"}"))
//  }
//}
//
//object mockAuthorityDataConnector extends MarriageAllowanceDESConnector {
//  override val httpGet = WSHttp
//  override val httpPost = WSHttp
//  override val httpPut = WSHttp
//  override val serviceUrl = ""
//  override val urlHeaderEnvironment = ""
//  override val urlHeaderAuthorization = "foo"
//  override val metrics = Metrics
//
//  override def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)
//                                                     (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
//    Future.failed(new BadRequestException("{\"reason\": \"User does not have authority to retrieve requested Participant 1 record\"}"))
//  }
//}
//
//object mockEmailConnector extends EmailConnector {
//  override val httpPost = WSHttp
//  override val emailUrl = "bar"
//  override def sendEmail(sendEmailRequest: SendEmailRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
//    Future(HttpResponse(200,None))
//  }
//}

