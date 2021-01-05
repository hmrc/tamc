/*
 * Copyright 2021 HM Revenue & Customs
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

import errors.{DataRetrievalError, ResponseValidationError}
import metrics.TamcMetrics
import models._
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsValue, JsonValidationError}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.{ExecutionContext, Future}

trait MarriageAllowanceConnector {

  val ProcessingOK = 1
  val ErrorReturnCode = -1011
  val NinoNotFound = 2016
  val MultipleNinosInMergeTrail = 2017
  val ConfidenceCheck = 2018
  val NinoRequired = 2039
  val OnlyOneNinoOrTempReference = 2040
  val SurnameNotSupplied = 2061

  val serviceUrl: String
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String
  val metrics: TamcMetrics
  def url(path: String) = s"$serviceUrl$path"
  def ninoWithoutSpaces(nino: Nino) = nino.value.replaceAll(" ", "")
  val logger: Logger

  def handleValidationError[A]: Seq[(JsPath, scala.Seq[JsonValidationError])] => Left[DataRetrievalError, A] =  err => {

    val extractValidationErrors: Seq[(JsPath, scala.Seq[JsonValidationError])] => String = errors => {
      errors.map {
        case (path, List(validationError: ValidationError, _*)) => s"$path: ${validationError.message}"
      }.mkString(", ").trim
    }

    logger.error(s"Not able to parse the response received from DES with error ${extractValidationErrors(err)}")
    Left(ResponseValidationError)
  }

  def buildHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(urlHeaderAuthorization))).withExtraHeaders("Environment" -> urlHeaderEnvironment)

  def findCitizen(nino: Nino)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue]
  def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue]
  def findRecipient(findRecipientRequest: FindRecipientRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[DataRetrievalError, UserRecord]]
  def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse]
  def updateAllowanceRelationship(updateRelationshipRequest: DesUpdateRelationshipRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse]

}
