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

import errors._
import models._
import play.api.Mode.Mode
import play.api.data.validation.ValidationError
import play.api.http.Status._
import play.api.libs.json.{JsPath, JsValue}
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import utils.WSHttp

import scala.concurrent.{ExecutionContext, Future}

object MarriageAllowanceDataConnector extends MarriageAllowanceDataConnector with ServicesConfig {
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override val httpGet = WSHttp
  override val httpPost = WSHttp
  override val httpPut = WSHttp
  override val serviceUrl = baseUrl("marriage-allowance-des")
  override val urlHeaderEnvironment = config("marriage-allowance-des").getString("environment").get
  override val urlHeaderAuthorization = s"Bearer ${config("marriage-allowance-des").getString("authorization-token").get}"

}

trait MarriageAllowanceDataConnector extends MarriageAllowanceConnector {

  def findCitizen(nino: Nino)(implicit ec: ExecutionContext): Future[JsValue] = {
    implicit val hc = createHeaderCarrier
    val path = url(s"/marriage-allowance/citizen/${nino}")
    httpGet.GET[JsValue](path)
  }

  def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit ec: ExecutionContext): Future[JsValue] = {
    implicit val hc = createHeaderCarrier
    val path = url(s"/marriage-allowance/citizen/${cid}/relationships?includeHistoric=${includeHistoric}")
    httpGet.GET[JsValue](path)
  }

  def findRecipient(nino: String, findRecipientRequestDes: FindRecipientRequestDes)(implicit ec: ExecutionContext): Future[Either[FindRecipientRetrievalError, UserRecord]] = {
    implicit val hc = createHeaderCarrier

    val genderQueryString = findRecipientRequestDes.gender.fold("")(gender => s"&gender=${utils.encodeQueryStringValue(gender)}")
    val query = s"surname=${utils.encodeQueryStringValue(findRecipientRequestDes.surname)}&forename1=${utils.encodeQueryStringValue(findRecipientRequestDes.forename1)}$genderQueryString"

    def extractValidationErrors: Seq[(JsPath, scala.Seq[ValidationError])] => String = errors => {
      errors.map {
        case (path, List(validationError: ValidationError, _*)) => s"$path: ${validationError.message}"
      }.mkString(", ").trim
    }

    def handleError: Seq[(JsPath, scala.Seq[ValidationError])] => Left[FindRecipientRetrievalError, UserRecord] =  err => {
      Logger.error(s"Not able to parse the response received from DES with error ${extractValidationErrors(err)}")
      Left(ResponseValidationError)
    }

    //TODO move away from Magic Numbers
    def evaluateCodes(findRecipientResponseDES: FindRecipientResponseDES): Either[FindRecipientRetrievalError, UserRecord] = {
      (findRecipientResponseDES.returnCode, findRecipientResponseDES.reasonCode) match {
        case(1, 1) => Right(UserRecord(findRecipientResponseDES.instanceIdentifier, findRecipientResponseDES.updateTimeStamp))
        case(_, _) => Left(UnhandledStatusError)
      }
    }

    implicit val httpRead = new HttpReads[Either[FindRecipientRetrievalError, UserRecord]]{

      //TODO logging
    override def read(method: String, url: String, response: HttpResponse): Either[FindRecipientRetrievalError, UserRecord] =
      response.status match {
        case OK => response.json.validate[FindRecipientResponseDES].fold(handleError(_), evaluateCodes(_))
        case _ => Left(UnhandledStatusError)
      }
    }

    val path = url(s"/marriage-allowance/citizen/${nino}/check?${query}")
    httpGet.GET[Either[FindRecipientRetrievalError, UserRecord]](path)
  }

  def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    val path = url(s"/marriage-allowance/02.00.00/citizen/${createRelationshipRequest.recipientCid}/relationship/${relType}")
    httpPost.POST(path, createRelationshipRequest)
  }

  def updateAllowanceRelationship(updateRelationshipRequest: DesUpdateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    val path = url(s"/marriage-allowance/citizen/${updateRelationshipRequest.participant1.instanceIdentifier}/relationship")
    httpPut.PUT(path, updateRelationshipRequest)
  }
}
