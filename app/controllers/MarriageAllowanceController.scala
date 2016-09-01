/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import errors.ServiceError
import errors.TransferorDeceasedError
import models.CreateRelationshipResponse
import models.FindRecipientRequest
import models.GetRelationshipResponse
import models.RelationshipRecordStatusWrapper
import models.RelationshipRecordWrapper
import models.ResponseStatus
import models.UpdateRelationshipRequestHolder
import models.UpdateRelationshipResponse
import models.UserRecord
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Action
import services.MarriageAllowanceService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.microservice.controller.BaseController
import errors.UpdateRelationshipError
import uk.gov.hmrc.play.http.{ NotFoundException, BadRequestException, InternalServerException, ServiceUnavailableException }
import errors.ErrorResponseStatus
import models.MultiYearCreateRelationshipRequestHolder
import models.TaxYear
import errors.RecipientDeceasedError

object MarriageAllowanceController extends MarriageAllowanceController {
  override val marriageAllowanceService = MarriageAllowanceService
}

trait MarriageAllowanceController extends BaseController {

  val marriageAllowanceService: MarriageAllowanceService

  def getRecipientRelationship(transferorNino: Nino) = Action.async(parse.json) { implicit request =>
    withJsonBody[FindRecipientRequest] { findRecipientRequest =>
      marriageAllowanceService.getRecipientRelationship(transferorNino, findRecipientRequest) map {
        case (recipientRecord: UserRecord, taxYears: List[TaxYear]) =>
          Ok(Json.toJson(GetRelationshipResponse(
            user_record = Some(recipientRecord),
            availableYears = Some(taxYears),
            status = ResponseStatus(status_code = "OK"))))
      } recover {
        case error =>
          error match {
            case serviceError: ServiceError =>
              Logger.warn("getRecipientRelationship failed with handled error", error)
              Ok(Json.toJson(GetRelationshipResponse(
                status = ResponseStatus(status_code = "TAMC:ERROR:RECIPIENT-NOT-FOUND"))))
            case otherError =>
              Logger.error("getRecipientRelationship failed with unhandled error", error)
              Ok(Json.toJson(GetRelationshipResponse(
                status = ResponseStatus(status_code = "TAMC:ERROR:OTHER-ERROR"))))
          }
      }
    }
  }

  def createMultiYearRelationship(transferorNino: Nino, journey: String) = Action.async(parse.json) {
    implicit request =>
      withJsonBody[MultiYearCreateRelationshipRequestHolder] { createRelationshipRequestHolder =>
        marriageAllowanceService.createMultiYearRelationship(createRelationshipRequestHolder, journey) map {
          case _ =>
            Ok(Json.toJson(CreateRelationshipResponse(
              status = ResponseStatus(status_code = "OK"))))
        } recover {
          case error =>
            error match {
              case recipientDeceased: RecipientDeceasedError =>
                Logger.warn("Create Relationship failed with 400 recipient deceased", error)
                Ok(Json.toJson(CreateRelationshipResponse(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))))
            }
        }
      }
  }

  def listRelationship(transferorNino: Nino) = Action.async { implicit request =>
    marriageAllowanceService.listRelationship(transferorNino) map {
      case relationshipList: RelationshipRecordWrapper =>
        Ok(Json.toJson(RelationshipRecordStatusWrapper(relationship_record = relationshipList, status = ResponseStatus(status_code = "OK"))))
    } recover {
      case error =>
        error match {
          case deceasedError: TransferorDeceasedError =>
            Logger.warn("listRelationship failed with deceased case error", error)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = "TAMC:ERROR:TRANSFEROR-NOT-FOUND"))))
          case serviceError: ServiceError =>
            Logger.warn("listRelationship failed with transferor not found error", error)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = "TAMC:ERROR:TRANSFEROR-NOT-FOUND"))))
          case notFound: NotFoundException =>
            Logger.warn("List Relationship failed with 404 not found error", error)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.CITIZEN_NOT_FOUND))))
          case badRequest: BadRequestException =>
            Logger.warn("List Relationship failed with 400 bad request error", error)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))))
          case internalServerError: InternalServerException =>
            Logger.warn("List Relationship failed with 500 internal server error", error)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.SERVER_ERROR))))
          case serviceUnavailable: ServiceUnavailableException =>
            Logger.warn("List Relationship failed with 503 service unavailable error", error)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.SERVICE_UNAVILABLE))))
          case otherError =>
            Logger.error("List Relationship failed with unhandled error", error)
            throw otherError
        }
    }
  }

  def updateRelationship(transferorNino: Nino) = Action.async(parse.json) {
    implicit request =>
      withJsonBody[UpdateRelationshipRequestHolder] {
        updateRelationshipRequestHolder =>
          marriageAllowanceService.updateRelationship(updateRelationshipRequestHolder) map {
            case _ => {
              Ok(Json.toJson(UpdateRelationshipResponse(
                status = ResponseStatus(status_code = "OK"))))
            }
          } recover {
            case error =>
              error match {
                case recipientDeceased: RecipientDeceasedError =>
                  Logger.warn("Update Relationship failed with 400 recipient deceased", error)
                  Ok(Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))))
                case relationshipError: UpdateRelationshipError =>
                  Logger.warn("Update Relationship failed with UpdateRelationshipError(runtime) error", error)
                  Ok(Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = ErrorResponseStatus.CANNOT_UPDATE_RELATIONSHIP))))
                case otherError =>
                  Logger.error("updateRelationship failed with unhandled error", error)
                  throw otherError
              }
          }
      }
  }
}
