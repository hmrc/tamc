/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import models._
import services.MarriageAllowanceService
import test_utils.TestUtility
import uk.gov.hmrc.http.{BadGatewayException, CoreGet, HeaderCarrier, HttpResponse, _}
import uk.gov.hmrc.play.test.UnitSpec
import Fixtures._
import com.codahale.metrics.Timer
import errors.{MultiYearCreateRelationshipError, RecipientDeceasedError}
import metrics.Metrics
import models.ApiType.ApiType
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import uk.gov.hmrc.time.TaxYearResolver
import utils.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MarriageAllowanceServiceTest extends UnitSpec with TestUtility with OneAppPerSuite{

  override implicit lazy val app: Application = fakeApplication


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

class FakeDeceasedMarriageAllowanceService extends MarriageAllowanceService {
  override val dataConnector = mockDeceasedDataConnector
  override val emailConnector = mockEmailConnector
  override val metrics = Metrics
  override val taxYearResolver = TaxYearResolver
  override val startTaxYear = 2015
  override val maSupportedYearsCount = 5

}

class FakeAuthorityMarriageAllowanceService extends MarriageAllowanceService {
  override val dataConnector = mockAuthorityDataConnector
  override val emailConnector = mockEmailConnector
  override val metrics = Metrics
  override val taxYearResolver = TaxYearResolver
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
}
