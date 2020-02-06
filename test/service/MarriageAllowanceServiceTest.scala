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
import connectors.{EmailConnector, MarriageAllowanceDESConnector, MarriageAllowanceDataConnector}
import metrics.Metrics
import models.ApiType.ApiType
import models._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.MarriageAllowanceService
import test_utils.TestUtility
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, _}
import uk.gov.hmrc.play.test.UnitSpec
import utils.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

//TODO remove the need for TestUtility
class MarriageAllowanceServiceTest extends UnitSpec with TestUtility with GuiceOneAppPerSuite with MockitoSugar{

//  val generatedNino = new Generator().nextNino
//  val findRecipientRequest = FindRecipientRequest(name = "testForename1", lastName = "testLastName", gender = Gender("M"), nino = generatedNino)
//
//  val service = new MarriageAllowanceService {
//    override val dataConnector = mock[MarriageAllowanceDESConnector]
//    override val emailConnector = mock[EmailConnector]
//    override val metrics = mock[Metrics]
//    override val startTaxYear = START_TAX_YEAR
//    override val maSupportedYearsCount = MA_SUPPORTED_YEARS_COUNT
//  }

//  "getRecipientRelationship" should {
//
//    "return a valid response" in {
//
//      val userRecord = UserRecord(cid = 123456789, timestamp = "20200116155359011123")
//      val taxYearModel = TaxYear(2019)
//
//      val expectedResponse = (userRecord, List(taxYearModel))
//
//      when(service.dataConnector.findRecipient(generatedNino, findRecipientRequest)(ArgumentMatchers.any()))
//        .thenReturn(Future.successful(Right(userRecord)))
//
//      when(service.dataConnector.findCitizen(generatedNino)(ArgumentMatchers.any()))
//        .thenReturn(Future.successful(Right(userRecord)))
//
//    }
//
//  }

  "when request is sent with deceased recipient in MarriageAllowanceService" should {
    "return a BadRequestException" in {
        val service = new FakeDeceasedMarriageAllowanceService
        val multiYearCreateRelationshipRequest = MultiYearCreateRelationshipRequestHolderFixture.multiYearCreateRelationshipRequestHolder
        val response = service.createMultiYearRelationship(multiYearCreateRelationshipRequest, "GDS")(new HeaderCarrier(), implicitly)
        //noException should be thrownBy await(response)
        intercept[BadRequestException] {
          await(response)
        }
    }
  }

}

//TODO fix
class FakeDeceasedMarriageAllowanceService extends MarriageAllowanceService {
  override val dataConnector = mockDeceasedDataConnector
  override val emailConnector = mockEmailConnector
  override val metrics = Metrics
  override val startTaxYear = 2015
  override val maSupportedYearsCount = 5

}

//TODO fix
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

  override def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
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

  override def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
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
