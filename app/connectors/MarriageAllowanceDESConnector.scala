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

import java.util.UUID

import errors._
import metrics.Metrics
import models._
import play.api.Mode.Mode
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import utils.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait MarriageAllowanceDESConnector extends MarriageAllowanceConnector {

  def logger = Logger("marriageAllowanceDESConnector")

  def findCitizen(nino: Nino)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] = {
    val path = url(s"/marriage-allowance/citizen/${nino}")
    httpGet.GET[JsValue](path)(implicitly, buildHeaderCarrier(hc), ec)
  }

  def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] = {
    val path = url(s"/marriage-allowance/citizen/${cid}/relationships?includeHistoric=${includeHistoric}")
    httpGet.GET[JsValue](path)
  }

  def findRecipient(findRecipientRequest: FindRecipientRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[DataRetrievalError, UserRecord]] = {

    def evaluateCodes(findRecipientResponseDES: FindRecipientResponseDES): Either[DataRetrievalError, UserRecord] = {

      (findRecipientResponseDES.returnCode, findRecipientResponseDES.reasonCode) match {
        case(ProcessingOK, ProcessingOK) => {
          metrics.incrementSuccessCounter(ApiType.FindRecipient)
          Right(UserRecord(findRecipientResponseDES.instanceIdentifier, findRecipientResponseDES.updateTimeStamp))
        }
        case codes @ (ErrorReturnCode, NinoNotFound) => {
          val codedErrorResponse = FindRecipientCodedErrorResponse(codes._1, codes._2, "Nino not found and Nino not found in merge trail")
          logger.warn(codedErrorResponse.errorMessage)
          metrics.incrementSuccessCounter(ApiType.FindRecipient)
          Left(codedErrorResponse)
        }
        case codes @ (ErrorReturnCode, MultipleNinosInMergeTrail) => {
          val codedErrorResponse = FindRecipientCodedErrorResponse(codes._1, codes._2, "Nino not found and Nino found in multiple merge trails")
          logger.warn(codedErrorResponse.errorMessage)
          metrics.incrementSuccessCounter(ApiType.FindRecipient)
          Left(codedErrorResponse)
        }
        case codes @ (ErrorReturnCode, ConfidenceCheck) => {
          val codedErrorResponse = FindRecipientCodedErrorResponse(codes._1, codes._2, "Confidence check failed")
          logger.error(codedErrorResponse.errorMessage)
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          Left(codedErrorResponse)
        }
        case codes @ (ErrorReturnCode, NinoRequired) => {
          val codedErrorResponse = FindRecipientCodedErrorResponse(codes._1, codes._2, "Nino must be supplied")
          logger.error(codedErrorResponse.errorMessage)
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          Left(codedErrorResponse)
        }
        case codes @ (ErrorReturnCode, OnlyOneNinoOrTempReference) => {
          val codedErrorResponse = FindRecipientCodedErrorResponse(codes._1, codes._2, "Only one of Nino or Temporary Reference must be supplied")
          logger.error(codedErrorResponse.errorMessage)
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          Left(codedErrorResponse)
        }
        case codes @ (ErrorReturnCode, SurnameNotSupplied) => {
          val codedErrorResponse = FindRecipientCodedErrorResponse(codes._1, codes._2, "Confidence Check Surname not supplied")
          logger.error(codedErrorResponse.errorMessage)
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          Left(codedErrorResponse)
        }
        case(returnCode, reasonCode) => {
          logger.error(s"Unknown response code returned from DES: ReturnCode=$returnCode, ReasonCode=$reasonCode")
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          Left(UnhandledStatusError)
        }
      }
    }

    val httpRead = new HttpReads[Either[DataRetrievalError, UserRecord]]{

      override def read(method: String, url: String, response: HttpResponse): Either[DataRetrievalError, UserRecord] =
        response.status match {
          case OK => response.json.validate[FindRecipientResponseDES].fold(handleValidationError(_), evaluateCodes(_))
          case BAD_REQUEST => {
            metrics.incrementFailedCounter(ApiType.FindRecipient)
            Left(BadRequestError)
          }
          case TOO_MANY_REQUESTS => {
            metrics.incrementFailedCounter(ApiType.FindRecipient)
            Left(TooManyRequestsError)
          }
          case INTERNAL_SERVER_ERROR => {
            metrics.incrementFailedCounter(ApiType.FindRecipient)
            logger.error(s" Internal Server Error received from DES: ${response.body}")
            Left(ServerError)
          }
          case SERVICE_UNAVAILABLE => {
            metrics.incrementFailedCounter(ApiType.FindRecipient)
            logger.error("Service Unavailable returned from DES")
            Left(ServiceUnavailableError)
          }
          case 499 | GATEWAY_TIMEOUT => {
            metrics.incrementFailedCounter(ApiType.FindRecipient)
            logger.error("Timeout Error has been received from DES")
            Left(TimeOutError)
          }
          case BAD_GATEWAY => {
            metrics.incrementFailedCounter(ApiType.FindRecipient)
            logger.error("Bad Gateway Error returned from DES")
            Left(BadGatewayError)
          }
          case _ => {
            metrics.incrementFailedCounter(ApiType.FindRecipient)
            logger.error(s"Des has returned Unhandled Status Code: ${response.status}")
            Left(UnhandledStatusError)
          }
        }
    }

    val updatedHeaderCarrier: HeaderCarrier = buildHeaderCarrier(hc).withExtraHeaders("CorrelationId" -> UUID.randomUUID().toString)
    val nino = ninoWithoutSpaces(findRecipientRequest.nino)
    val path = url(s"/marriage-allowance/citizen/$nino/check")
    val findRecipientRequestDes = FindRecipientRequestDes(findRecipientRequest)

    metrics.incrementTotalCounter(ApiType.FindRecipient)
    val timer = metrics.startTimer(ApiType.FindRecipient)

    httpPost.POST(path, findRecipientRequestDes)(implicitly, httpRead, updatedHeaderCarrier, ec).map { response =>
      timer.stop()
      response

    } recover {
      case _: GatewayTimeoutException => {
        metrics.incrementFailedCounter(ApiType.FindRecipient)
        timer.stop()
        Left(TimeOutError)
      }
      case _: BadGatewayException => {
        metrics.incrementFailedCounter(ApiType.FindRecipient)
        timer.stop()
        Left(BadGatewayError)
      }
      case NonFatal(e) => {
        metrics.incrementFailedCounter(ApiType.FindRecipient)
        timer.stop()
        throw e
      }
    }
  }

  def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val path = url(s"/marriage-allowance/02.00.00/citizen/${createRelationshipRequest.recipientCid}/relationship/${relType}")
    httpPost.POST(path, createRelationshipRequest)(MultiYearDesCreateRelationshipRequest.multiYearWrites, HttpReads.readRaw, buildHeaderCarrier(hc), ec)
  }

  def updateAllowanceRelationship(updateRelationshipRequest: DesUpdateRelationshipRequest)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val path = url(s"/marriage-allowance/citizen/${updateRelationshipRequest.participant1.instanceIdentifier}/relationship")
    httpPut.PUT(path, updateRelationshipRequest)(DesUpdateRelationshipRequest.formats, HttpReads.readRaw, buildHeaderCarrier(hc), ec)
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
  override val metrics = Metrics

}
