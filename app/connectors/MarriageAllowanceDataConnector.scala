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

package connectors

import javax.inject.Inject
import models.{Cid, DesUpdateRelationshipRequest, FindRecipientRequest, MultiYearDesCreateRelationshipRequest}
import play.api.Mode.Mode
import play.api.libs.json.JsValue
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class MarriageAllowanceDataConnector @Inject()(httpClient: HttpClient,
                                               environment: Environment,
                                               val runModeConfiguration: Configuration) extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  val serviceUrl: String = baseUrl("marriage-allowance-des")
  val urlHeaderEnvironment: String = config("marriage-allowance-des").getString("environment").get
  val urlHeaderAuthorization: String = s"Bearer ${config("marriage-allowance-des").getString("authorization-token").get}"

  def url(path: String) = s"$serviceUrl$path"

  private def createHeaderCarrier = HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

  def findCitizen(nino: Nino)(implicit ec: ExecutionContext): Future[JsValue] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val path = url(s"/marriage-allowance/citizen/$nino")
    httpClient.GET[JsValue](path)
  }

  def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit ec: ExecutionContext): Future[JsValue] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val path = url(s"/marriage-allowance/citizen/$cid/relationships?includeHistoric=$includeHistoric")
    httpClient.GET[JsValue](path)
  }

  def findRecipient(findRecipientRequest: FindRecipientRequest)(implicit ec: ExecutionContext): Future[JsValue] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val query = s"surname=${utils.encodeQueryStringValue(findRecipientRequest.lastName)}&forename1=${utils.encodeQueryStringValue(findRecipientRequest.name)}&gender=${utils.encodeQueryStringValue(findRecipientRequest.gender.gender)}"
    val nino = findRecipientRequest.nino.nino.replaceAll(" ", "")
    val path = url(s"/marriage-allowance/citizen/$nino/check?$query")
    httpClient.GET[JsValue](path)
  }

  def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val path = url(s"/marriage-allowance/02.00.00/citizen/${createRelationshipRequest.recipientCid}/relationship/$relType")
    httpClient.POST(path, createRelationshipRequest)
  }

  def updateAllowanceRelationship(updateRelationshipRequest: DesUpdateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val path = url(s"/marriage-allowance/citizen/${updateRelationshipRequest.participant1.instanceIdentifier}/relationship")
    httpClient.PUT(path, updateRelationshipRequest)
  }
}
