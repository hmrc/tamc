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

import connectors.MarriageAllowanceDataConnector
import javax.inject.Inject
import models.{Cid, DesCreateRelationshipRequest, DesUpdateRelationshipRequest, FindRecipientRequest}
import play.api.libs.json.JsValue
import play.api.{Configuration, Environment}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.DummyHttpResponse

import scala.concurrent.{ExecutionContext, Future}

class FakeMarriageAllowanceDataConnector @Inject()(httpClient: HttpClient,
                                                   environment: Environment,
                                                   configuration: Configuration,
                                                   runMode: RunMode) extends MarriageAllowanceDataConnector(httpClient, environment, configuration, runMode) {

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

    updateAllowanceRelationshipDataToTest = Some(data)
    updateAllowanceRelationshipDataToTestCount = updateAllowanceRelationshipDataToTestCount + 1
    super.updateAllowanceRelationship(data)
  }

  override def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit ec: ExecutionContext): Future[JsValue] = {
    super.listRelationship(cid, includeHistoric)
  }
}
