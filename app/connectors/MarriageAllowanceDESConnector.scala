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

import errors.{FindRecipientRetrievalError, ResponseValidationError}
import models._
import play.api.Mode.Mode
import play.api.data.validation.ValidationError
import play.api.http.Status
import play.api.libs.json.{JsPath, JsValue}
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import utils.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


trait MarriageAllowanceDESConnector extends MarriageAllowanceConnector {

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

  def findRecipient(nino: String, findRecipientRequest: FindRecipientRequestDes)(implicit ec: ExecutionContext): Future[Either[FindRecipientRetrievalError, FindRecipientResponseDES]] = {

    def extractValidationErrors: Seq[(JsPath, scala.Seq[ValidationError])] => String = errors => {
      errors.map {
        case (path, List(validationError: ValidationError, _*)) => s"$path: ${validationError.message}"
      }.mkString(", ").trim
    }

    def handleError: Seq[(JsPath, scala.Seq[ValidationError])] => Left[FindRecipientRetrievalError, FindRecipientResponseDES] =  err => {
      Logger.error(s"Not able to parse the response received from DES with error ${extractValidationErrors(err)}")
      Left(ResponseValidationError)
    }


    implicit val hc = createHeaderCarrier
    //TODO config derived
    val path = url(s"/marriage-allowance/citizen/${nino}/check")

    implicit val httpRead = new HttpReads[Either[FindRecipientRetrievalError, FindRecipientResponseDES]]{

      override def read(method: String, url: String, response: HttpResponse): Either[FindRecipientRetrievalError, FindRecipientResponseDES] =
        response.status match {
          case Status.OK => response.json.validate[FindRecipientResponseDES].fold(handleError(_), Right(_))
        }
    }

    httpPost.POST(path, findRecipientRequest)
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

object MarriageAllowanceDESConnector extends MarriageAllowanceDESConnector with ServicesConfig {
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override val httpGet = WSHttp
  override val httpPost = WSHttp
  override val httpPut = WSHttp
  override val serviceUrl = baseUrl("marriage-allowance-des")
  override val urlHeaderEnvironment = config("marriage-allowance-des").getString("environment").get
  override val urlHeaderAuthorization = s"Bearer ${config("marriage-allowance-des").getString("authorization-token").get}"

}
