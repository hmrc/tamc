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

package connectors

import errors.DataRetrievalError
import models._
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.{ExecutionContext, Future}

trait MarriageAllowanceConnector {

  val httpGet: HttpGet
  val httpPost: HttpPost
  val httpPut: HttpPut
  val serviceUrl: String
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String
  def url(path: String) = s"$serviceUrl$path"
  def ninoWithoutSpaces(nino: Nino) = nino.value.replaceAll(" ", "")

  def createHeaderCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = Seq(("Environment" -> urlHeaderEnvironment)), authorization = Some(Authorization(urlHeaderAuthorization)))

  def findCitizen(nino: Nino)(implicit ec: ExecutionContext): Future[JsValue]
  def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit ec: ExecutionContext): Future[JsValue]
  def findRecipient(nino: Nino, findRecipientRequest: FindRecipientRequestDes)(implicit ec: ExecutionContext): Future[Either[DataRetrievalError, UserRecord]]
  def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse]
  def updateAllowanceRelationship(updateRelationshipRequest: DesUpdateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse]

}
