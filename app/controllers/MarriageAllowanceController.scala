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

package controllers

import com.google.inject.Inject
import controllers.auth.AuthAction
import errors.ErrorResponseStatus._
import errors._
import models._
import play.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.MarriageAllowanceService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class MarriageAllowanceController @Inject()(marriageAllowanceService: MarriageAllowanceService,
                                            authAction: AuthAction,
                                            cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getRecipientRelationship(transferorNino: Nino): Action[JsValue] = authAction.async(parse.json) { implicit request =>
    withJsonBody[FindRecipientRequest] { findRecipientRequest =>
      marriageAllowanceService.getRecipientRelationship(transferorNino, findRecipientRequest) map {
        case Right((recipientRecord, taxYears)) =>
          Ok(Json.toJson(GetRelationshipResponse(
            user_record = Some(recipientRecord),
            availableYears = Some(taxYears),
            status = ResponseStatus(status_code = "OK"))))
        case Left(_: DataRetrievalError) => Ok(Json.toJson(GetRelationshipResponse(
          status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND))))
      } recover {
        case error: ServiceError =>
          Logger.warn("getRecipientRelationship failed with handled error", error.getMessage)
          Ok(Json.toJson(GetRelationshipResponse(
            status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND))))
        case error: TransferorDeceasedError =>
          Logger.warn("getRecipientRelationship failed with handled error", error.getMessage)
          Ok(Json.toJson(GetRelationshipResponse(
            status = ResponseStatus(status_code = TRANSFERER_DECEASED))))
        case error =>
          Logger.error("getRecipientRelationship failed with unhandled error", error.getMessage)
          Ok(Json.toJson(GetRelationshipResponse(
            status = ResponseStatus(status_code = OTHER_ERROR))))
      }
    }
  }

  def createMultiYearRelationship(transferorNino: Nino, journey: String): Action[JsValue] = authAction.async(parse.json) {
    implicit request =>
      withJsonBody[MultiYearCreateRelationshipRequestHolder] { createRelationshipRequestHolder =>
        marriageAllowanceService.createMultiYearRelationship(createRelationshipRequestHolder, journey) map {
          _ =>
            Ok(Json.toJson(CreateRelationshipResponse(
              status = ResponseStatus(status_code = "OK"))))
        } recover {
          case badRequest: BadRequestException if badRequest.message.contains("Participant is deceased") =>
            Logger.warn("createMultiYearRelationship failed with participant deceased: ", badRequest.getMessage)
            Ok(Json.toJson(CreateRelationshipResponse(
              status = ResponseStatus(status_code = RECIPIENT_DECEASED))))
          case conflict: Upstream4xxResponse if conflict.message.contains("Cannot update as Participant") =>
            Logger.warn("createMultiYearRelationship failed with conflict 409: ", conflict.getMessage)
            Ok(Json.toJson(CreateRelationshipResponse(
              status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED))))
          case ex: Upstream5xxResponse if ex.message.contains("LTM000503") =>
            Logger.warn("createMultiYearRelationship failed with LTM000503: ", ex.getMessage)
            Ok(Json.toJson(CreateRelationshipResponse(
              status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED))))
          case ex =>
            Logger.error("createMultiYearRelationship failed with unhandled error: ", ex.getMessage)
            throw ex
        }
      }
  }

  def listRelationship(transferorNino: Nino): Action[AnyContent] = authAction.async { implicit request =>
    marriageAllowanceService.listRelationship(transferorNino) map {
      relationshipList: RelationshipRecordWrapper =>
        Ok(Json.toJson(RelationshipRecordStatusWrapper(relationship_record = relationshipList, status = ResponseStatus(status_code = "OK"))))
    } recover {
      case error =>
        error match {
          case _: TransferorDeceasedError =>
            Logger.warn("listRelationship failed with deceased case error", error.getMessage)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = TRANSFEROR_NOT_FOUND))))
          case _: ServiceError =>
            Logger.warn("listRelationship failed with transferor not found error", error.getMessage)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = TRANSFEROR_NOT_FOUND))))
          case _: NotFoundException =>
            Logger.warn("listRelationship failed with 404 not found error", error.getMessage)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = CITIZEN_NOT_FOUND))))
          case _: BadRequestException =>
            Logger.warn("listRelationship failed with 400 bad request error", error.getMessage)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))))
          case WithStatusCode(INTERNAL_SERVER_ERROR) =>
            Logger.warn("listRelationship failed with 500 internal server error", error.getMessage)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = SERVER_ERROR))))
          case WithStatusCode(SERVICE_UNAVAILABLE) =>
            Logger.warn("listRelationship failed with 503 service unavailable error", error.getMessage)
            Ok(Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.SERVICE_UNAVILABLE))))
          case otherError =>
            Logger.error("listRelationship failed with unhandled error", error.getMessage)
            throw otherError
        }
    }
  }

  def updateRelationship(transferorNino: Nino): Action[JsValue] = authAction.async(parse.json) {
    implicit request =>
      withJsonBody[UpdateRelationshipRequestHolder] {
        updateRelationshipRequestHolder =>
          marriageAllowanceService.updateRelationship(updateRelationshipRequestHolder) map {
            _ => {
              Ok(Json.toJson(UpdateRelationshipResponse(
                status = ResponseStatus(status_code = "OK"))))
            }
          } recover {
            case error =>
              error match {
                case _: RecipientDeceasedError =>
                  Logger.warn("Update Relationship failed with 400 recipient deceased", error.getMessage)
                  Ok(Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))))
                case _: UpdateRelationshipError =>
                  Logger.warn("Update Relationship failed with UpdateRelationshipError(runtime) error", error.getMessage)
                  Ok(Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = CANNOT_UPDATE_RELATIONSHIP))))
                case otherError =>
                  Logger.error("updateRelationship failed with unhandled error", error.getMessage)
                  throw otherError
              }
          }
      }
  }
}
