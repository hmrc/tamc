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

package services

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import config.ApplicationConfig
import connectors.EmailConnector
import connectors.MarriageAllowanceDataConnector
import errors.CheckRelationshipError
import errors.CreateRelationshipError
import errors.FindRecipientError
import errors.FindTransferorError
import models.Cid
import models.CitizenName
import models.CreateRelationshipNotificationRequest
import models.CreateRelationshipRequest
import models.CreateRelationshipRequestHolder
import models.DesCreateRelationshipRequest
import models.FindRecipientRequest
import models.SendEmailRequest
import models.Timestamp
import models.UserRecord
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.http.HeaderCarrier
import errors.TransferorDeceasedError
import errors.RecipientDeceasedError
import models.RelationshipRecord
import play.api.libs.json.Json
import models.RelationshipRecordWrapper
import models.DesUpdateRelationshipRequest
import models.UpdateRelationshipRequestHolder
import models.UpdateRelationshipNotificationRequest
import errors.UpdateRelationshipError
import metrics.Metrics
import models.ApiType
import models.DesRelationshipInformation
import java.text.SimpleDateFormat
import java.util.Calendar
import models.MultiYearCreateRelationshipRequestHolder
import models.MultiYearCreateRelationshipRequest
import models.MultiYearDesCreateRelationshipRequest
import uk.gov.hmrc.time.TaxYearResolver
import models.DesCreateRelationshipRequest
import models.MultiYearCreateRelationshipResponse
import errors.MultiYearCreateRelationshipError
import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDate
import models.EligibleTaxYearListResponse
import models.TaxYear

object MarriageAllowanceService extends MarriageAllowanceService {
  override val dataConnector = MarriageAllowanceDataConnector
  override val emailConnector = EmailConnector
  override val metrics = Metrics
  override val taxYearResolver = TaxYearResolver
  override val startTaxYear = ApplicationConfig.START_TAX_YEAR
}

trait MarriageAllowanceService {

  val dataConnector: MarriageAllowanceDataConnector
  val emailConnector: EmailConnector
  val metrics: Metrics
  val taxYearResolver: TaxYearResolver
  val startTaxYear: Int

  def getRecipientRelationship(transferorNino: Nino, findRecipientRequest: FindRecipientRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(UserRecord, List[TaxYear])] = {
    for {
      recipientRecord <- getRecipientRecord(findRecipientRequest)
      //recipientRecordWithRelationship <- getRelationshipRecord(recipientRecord)
      recipientRelationshipList <- listRelationship(recipientRecord.cid)
      transferorRecord <- getTransferorRecord(transferorNino) //TODO may be get transfer CID from FE and call listRelationship(transferorRecord.cid) directly --> depends on frontend implementation
      transferorRelationshipList <- listRelationship(transferorRecord.cid)
      transferorYears <- convertToAvailedYears(transferorRelationshipList)
      recipientYears <- convertToAvailedYears(recipientRelationshipList)
      eligibleYearsBasdOnDoM <- getEligibleTaxYearList(findRecipientRequest.dateOfMarriage.get) //TODO convert dateOfMarriage to non-optional in Phase-3
      years <- getListOfEligibleTaxYears(transferorYears, recipientYears, eligibleYearsBasdOnDoM)
    } yield { (recipientRecord, years) }
  }

  def createMultiYearRelationship(createRelationshipRequestHolder: MultiYearCreateRelationshipRequestHolder, journey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    for {
      templateId <- getEmailTemplateId(createRelationshipRequestHolder.request.taxYears, createRelationshipRequestHolder.notification.welsh)
      _ <- handleMultiYearRequests(createRelationshipRequestHolder.request)
      sendEmailRequest <- transformEmailRequest(createRelationshipRequestHolder.notification, templateId)
      _ <- sendEmail(sendEmailRequest)
    } yield { Unit }
  }

  private def getEmailTemplateId(taxYears: List[Int], isWelsh: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    Future {
      isWelsh match {
        case true =>
          if (taxYears.size == 1 && taxYears.contains(TaxYearResolver.currentTaxYear))
            ApplicationConfig.EMAIL_APPLY_CURRENT_TAXYEAR_WELSH_TEMPLATE_ID
          else if (taxYears.size > 1 && taxYears.contains(TaxYearResolver.currentTaxYear))
            ApplicationConfig.EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID
          else
            ApplicationConfig.EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID
        case _ =>
          if (taxYears.size == 1 && taxYears.contains(TaxYearResolver.currentTaxYear))
            ApplicationConfig.EMAIL_APPLY_CURRENT_TAXYEAR_TEMPLATE_ID
          else if (taxYears.size > 1 && taxYears.contains(TaxYearResolver.currentTaxYear))
            ApplicationConfig.EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID
          else
            ApplicationConfig.EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID
      }
    }
  }

  private def handleMultiYearRequests(createRelationshipRequest: MultiYearCreateRelationshipRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    createRelationshipRequest.taxYears.headOption.fold {
      Future.successful(true)
    } {
      taxYear =>
        sendMultiYearCreateRelationshipRequest(createRelationshipRequest, taxYear).flatMap { response =>
          handleMultiYearRequests(
            createRelationshipRequest.copy(
              taxYears = createRelationshipRequest.taxYears.tail,
              recipient_timestamp = response.CID1Timestamp,
              transferor_timestamp = response.CID2Timestamp))
        }
    }
  }

  private def sendMultiYearCreateRelationshipRequest(createRelationshipRequest: MultiYearCreateRelationshipRequest, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MultiYearCreateRelationshipResponse] =
    createRelationshipRequest.taxYears.find { _ == taxYear }.fold {
      Future.failed[MultiYearCreateRelationshipResponse](new IllegalArgumentException())
    } {
      taxYear =>
        metrics.incrementTotalCounter(ApiType.CreateRelationship)
        val timer = metrics.startTimer(ApiType.CreateRelationship)

        val request = isCurrentTaxYear(taxYear) match {
          case true => ("active", convertRequest(
            createRelationshipRequest,
            startDate = None,
            endDate = None))
          case false => ("retrospective", convertRequest(
            createRelationshipRequest,
            startDate = Some(taxYearResolver.startOfTaxYear(taxYear).toString()),
            endDate = Some(taxYearResolver.endOfTaxYear(taxYear).toString())))
        }

        dataConnector.sendMultiYearCreateRelationshipRequest(request._1, request._2).map {
          httpResponse =>
            timer.stop()
            val json = httpResponse.json
            (json \ "status").as[String] match {
              case ("Processing OK") =>
                metrics.incrementSuccessCounter(ApiType.CreateRelationship)
                MultiYearCreateRelationshipResponse(
                  CID1Timestamp = (json \ "CID1Timestamp").as[Timestamp],
                  CID2Timestamp = (json \ "CID2Timestamp").as[Timestamp])
              case error if httpResponse.status == 400 =>
                throw RecipientDeceasedError("Service returned response with 400 - recipient deceased")
              case error =>
                throw MultiYearCreateRelationshipError(error)
            }
        }
    }

  private def isCurrentTaxYear(taxYear: Int): Boolean = {
    taxYearResolver.currentTaxYear == taxYear
  }

  private def convertRequest(request: MultiYearCreateRelationshipRequest, startDate: Option[String], endDate: Option[String]): MultiYearDesCreateRelationshipRequest =
    MultiYearDesCreateRelationshipRequest(
      recipientCid = request.recipient_cid.toString(),
      recipientTimestamp = request.recipient_timestamp,
      transferorCid = request.transferor_cid.toString(),
      transferorTimestamp = request.transferor_timestamp,
      startDate,
      endDate)

  def updateRelationship(updateRelationshipRequestHolder: UpdateRelationshipRequestHolder)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- sendUpdateRelationshipRequest(updateRelationshipRequestHolder.request)
      sendEmailRequest <- transformEmailForUpdateRequest(updateRelationshipRequestHolder)
      _ <- sendEmail(sendEmailRequest)
    } yield { Unit }
  }

  private def transformEmailRequest(notification: CreateRelationshipNotificationRequest, tempalteId: String): Future[SendEmailRequest] = {
    val emailRecipients: List[EmailAddress] = List(notification.email)
    val emailParameters: Map[String, String] = Map("full_name" -> notification.full_name)
    Future.successful(SendEmailRequest(templateId = tempalteId, to = emailRecipients, parameters = emailParameters, force = false))
  }

  private def transformEmailForUpdateRequest(updateRelationshipRequestHolder: UpdateRelationshipRequestHolder): Future[SendEmailRequest] = {
    val emailRecipients: List[EmailAddress] = List(updateRelationshipRequestHolder.notification.email)
    val startDate = getEmailTemplateId(updateRelationshipRequestHolder.request.relationship, updateRelationshipRequestHolder.notification.role, updateRelationshipRequestHolder.notification.welsh)._2
    val endDate = getEmailTemplateId(updateRelationshipRequestHolder.request.relationship, updateRelationshipRequestHolder.notification.role, updateRelationshipRequestHolder.notification.welsh)._3
    val emailTemplateId = getEmailTemplateId(updateRelationshipRequestHolder.request.relationship, updateRelationshipRequestHolder.notification.role, updateRelationshipRequestHolder.notification.welsh)._1
    val emailParameters: Map[String, String] = Map("full_name" -> updateRelationshipRequestHolder.notification.full_name, "startDate" -> startDate, "endDate" -> endDate)
    Future.successful(SendEmailRequest(templateId = emailTemplateId, to = emailRecipients, parameters = emailParameters, force = false))
  }

  private def getEmailTemplateId(relationship: DesRelationshipInformation, role: String, isWelsh: Boolean): (String, String, String) = {
    (relationship.relationshipEndReason, role, isWelsh) match {
      case (ApplicationConfig.REASON_CANCEL, _, true) => (ApplicationConfig.EMAIL_UPDATE_CANCEL_WELSH_TEMPLATE_ID, ApplicationConfig.START_DATE + (taxYearResolver.currentTaxYear + 1), ApplicationConfig.END_DATE + (taxYearResolver.currentTaxYear + 1))
      case (ApplicationConfig.REASON_REJECT, ApplicationConfig.ROLE_RECIPIENT, true) =>
        if (relationship.actualEndDate.contains(taxYearResolver.currentTaxYear.toString())) (ApplicationConfig.EMAIL_UPDATE_REJECT_WELSH_TEMPLATE_ID, "", "")
        else (ApplicationConfig.EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR_WELSH, "", "")
      case (ApplicationConfig.REASON_DIVORCE, ApplicationConfig.ROLE_TRANSFEROR, true) =>
        if (relationship.actualEndDate == getDateInRequiredFormat(true)) (ApplicationConfig.EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR_WELSH, ApplicationConfig.START_DATE + (taxYearResolver.currentTaxYear + 1), ApplicationConfig.END_DATE + (taxYearResolver.currentTaxYear + 1))
        else if (relationship.actualEndDate == getDateInRequiredFormat(false)) (ApplicationConfig.EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_WELSH_TEMPLATE_ID, ApplicationConfig.START_DATE + taxYearResolver.currentTaxYear, "") else
          (ApplicationConfig.EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR_WELSH, ApplicationConfig.START_DATE + taxYearResolver.currentTaxYear, ApplicationConfig.END_DATE + taxYearResolver.currentTaxYear)
      case (ApplicationConfig.REASON_DIVORCE, ApplicationConfig.ROLE_RECIPIENT, true) =>
        if (relationship.actualEndDate == getDateInRequiredFormat(true)) (ApplicationConfig.EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_WELSH_TEMPLATE_ID, ApplicationConfig.START_DATE + (taxYearResolver.currentTaxYear + 1), ApplicationConfig.END_DATE + (taxYearResolver.currentTaxYear + 1))
        else (ApplicationConfig.EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR_WELSH, "", ApplicationConfig.END_DATE + taxYearResolver.currentTaxYear)
      
      case (ApplicationConfig.REASON_CANCEL, _, _) => (ApplicationConfig.EMAIL_UPDATE_CANCEL_TEMPLATE_ID, ApplicationConfig.START_DATE + (taxYearResolver.currentTaxYear + 1), ApplicationConfig.END_DATE + (taxYearResolver.currentTaxYear + 1))
      case (ApplicationConfig.REASON_REJECT, ApplicationConfig.ROLE_RECIPIENT, _) =>
        if (relationship.actualEndDate.contains(taxYearResolver.currentTaxYear.toString())) (ApplicationConfig.EMAIL_UPDATE_REJECT_TEMPLATE_ID, "", "")
        else (ApplicationConfig.EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR, "", "")
      case (ApplicationConfig.REASON_DIVORCE, ApplicationConfig.ROLE_TRANSFEROR, _) =>
        if (relationship.actualEndDate == getDateInRequiredFormat(true)) (ApplicationConfig.EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR, ApplicationConfig.START_DATE + (taxYearResolver.currentTaxYear + 1), ApplicationConfig.END_DATE + (taxYearResolver.currentTaxYear + 1))
        else if (relationship.actualEndDate == getDateInRequiredFormat(false)) (ApplicationConfig.EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_TEMPLATE_ID, ApplicationConfig.START_DATE + taxYearResolver.currentTaxYear, "") else
          (ApplicationConfig.EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR, ApplicationConfig.START_DATE + taxYearResolver.currentTaxYear, ApplicationConfig.END_DATE + taxYearResolver.currentTaxYear)
      case (ApplicationConfig.REASON_DIVORCE, ApplicationConfig.ROLE_RECIPIENT, _) =>
        if (relationship.actualEndDate == getDateInRequiredFormat(true)) (ApplicationConfig.EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_TEMPLATE_ID, ApplicationConfig.START_DATE + (taxYearResolver.currentTaxYear + 1), ApplicationConfig.END_DATE + (taxYearResolver.currentTaxYear + 1))
        else (ApplicationConfig.EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR, "", ApplicationConfig.END_DATE + taxYearResolver.currentTaxYear)
    }
  }

  private def getDateInRequiredFormat(current: Boolean): String = {
    val DATE_FORMAT = "yyyyMMdd"
    val sdf = new SimpleDateFormat(DATE_FORMAT)
    val currentdate = Calendar.getInstance()
    current match {
      case true => sdf.format(currentdate.getTime())
      case false => {
        currentdate.add(Calendar.YEAR, -1)
        sdf.format(currentdate.getTime())
      }
    }

  }

  private def sendEmail(sendEmailRequest: SendEmailRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    emailConnector.sendEmail(sendEmailRequest) map {
      case _ =>
        Logger.info("Sending email")
    } recover {
      case error =>
        Logger.warn("Cannot send email", error)
    }
  }

  def listRelationship(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecordWrapper] = {
    for {
      citizenRecord <- getTransferorRecord(nino)
      relationshipList <- listRelationshipRecord(citizenRecord)
    } yield { relationshipList }
  }

  private def getTransferorRecord(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] = {
    metrics.incrementTotalCounter(ApiType.FindCitizen)
    val timer = metrics.startTimer(ApiType.FindCitizen)
    dataConnector.findCitizen(nino).map {
      json =>
        timer.stop()
        ((json
          \ "Jtpr1311PerDetailsFindcallResponse"
          \ "Jtpr1311PerDetailsFindExport"
          \ "OutWCbdParameters"
          \ "ReturnCode").as[Int],
          (json
            \ "Jtpr1311PerDetailsFindcallResponse"
            \ "Jtpr1311PerDetailsFindExport"
            \ "OutWCbdParameters"
            \ "ReasonCode").as[Int]) match {
              case (1, 1) =>
                metrics.incrementSuccessCounter(ApiType.FindCitizen)
                ((json
                  \ "Jtpr1311PerDetailsFindcallResponse"
                  \ "Jtpr1311PerDetailsFindExport"
                  \ "OutItpr1Person"
                  \ "DeceasedSignal").as[String]) match {
                    case ("N") =>
                      UserRecord(
                        cid = (json
                          \ "Jtpr1311PerDetailsFindcallResponse"
                          \ "Jtpr1311PerDetailsFindExport"
                          \ "OutItpr1Person"
                          \ "InstanceIdentifier").as[Cid],
                        timestamp = (json
                          \ "Jtpr1311PerDetailsFindcallResponse"
                          \ "Jtpr1311PerDetailsFindExport"
                          \ "OutItpr1Person"
                          \ "UpdateTimestamp").as[Timestamp],
                        name = Some(CitizenName(
                          (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "FirstForename").asOpt[String],
                          (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "Surname").asOpt[String])))
                    case (_) => throw new TransferorDeceasedError("Service returned response with deceased indicator as Y or null ")
                  }
              case (returnCode, reasonCode) =>
                metrics.incrementSuccessCounter(ApiType.FindCitizen)
                throw new FindTransferorError(returnCode, reasonCode)
            }
    }
  }

  private def getRecipientRecord(findRecipientRequest: FindRecipientRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] = {
    metrics.incrementTotalCounter(ApiType.FindRecipient)
    val timer = metrics.startTimer(ApiType.FindRecipient)
    dataConnector.findRecipient(findRecipientRequest).map {
      json =>
        timer.stop()
        ((json
          \ "Jfwk1012FindCheckPerNoninocallResponse"
          \ "Jfwk1012FindCheckPerNoninoExport"
          \ "OutWCbdParameters"
          \ "ReturnCode").as[Int],
          (json
            \ "Jfwk1012FindCheckPerNoninocallResponse"
            \ "Jfwk1012FindCheckPerNoninoExport"
            \ "OutWCbdParameters"
            \ "ReasonCode").as[Int]) match {
              case (1, 1) =>
                metrics.incrementSuccessCounter(ApiType.FindRecipient)
                UserRecord(
                  cid = (json
                    \ "Jfwk1012FindCheckPerNoninocallResponse"
                    \ "Jfwk1012FindCheckPerNoninoExport"
                    \ "OutItpr1Person"
                    \ "InstanceIdentifier").as[Cid],
                  timestamp = (json
                    \ "Jfwk1012FindCheckPerNoninocallResponse"
                    \ "Jfwk1012FindCheckPerNoninoExport"
                    \ "OutItpr1Person"
                    \ "UpdateTimestamp").as[Timestamp])
              case (returnCode, reasonCode) =>
                metrics.incrementSuccessCounter(ApiType.FindRecipient)
                throw new FindRecipientError(returnCode, reasonCode)
            }
    }
  }

  private def listRelationshipRecord(userRecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecordWrapper] = {
    metrics.incrementTotalCounter(ApiType.ListRelationship)
    val timer = metrics.startTimer(ApiType.ListRelationship)
    dataConnector.listRelationship(userRecord.cid).map {
      json =>
        timer.stop()
        Logger.debug("ListRelationship Json >> " + json) //TODO remove after E2E testing
        metrics.incrementSuccessCounter(ApiType.ListRelationship)
        Json.parse("" + json + "").as[RelationshipRecordWrapper].copy(userRecord = Some(userRecord))
    }
  }

  private def sendUpdateRelationshipRequest(updateRelationshipRequest: DesUpdateRelationshipRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    metrics.incrementTotalCounter(ApiType.UpdateRelationship)
    val timer = metrics.startTimer(ApiType.UpdateRelationship)
    dataConnector.updateAllowanceRelationship(updateRelationshipRequest).map {
      httpResponse =>
        timer.stop()
        val json = httpResponse.json
        ((json
          \ "participant1"
          \ "endDate").as[String],
          (json
            \ "participant2"
            \ "endDate").as[String],
            (json
              \ "relationship"
              \ "actualEndDate").as[String]) match {
                case recievedResponse if httpResponse.status == 200 =>
                  metrics.incrementSuccessCounter(ApiType.UpdateRelationship)
                  Future.successful(Unit)
                case recievedResponse if httpResponse.status == 400 =>
                  throw RecipientDeceasedError("Service returned response with 400 - recipient deceased")
                case _ => throw UpdateRelationshipError("An unexpected error has occured while updating the relationship")
              }
    }
  }

  private def listRelationship(cid: Cid)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecordWrapper] = {
    metrics.incrementTotalCounter(ApiType.ListRelationship)
    val timer = metrics.startTimer(ApiType.ListRelationship)
    dataConnector.listRelationship(cid).map {
      json =>
        timer.stop()
        metrics.incrementSuccessCounter(ApiType.ListRelationship)
        Json.parse("" + json + "").as[RelationshipRecordWrapper]
    }
  }

  private def convertToAvailedYears(relationshipRecordWrapper: RelationshipRecordWrapper)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] = {
    val format = DateTimeFormat.forPattern("yyyyMMdd");
    val relationships = relationshipRecordWrapper.relationshipRecordList
    var availedYears: List[Int] = List()
    for (record <- relationships) {
      val startDate = record.participant1StartDate
      val endDate = record.participant1EndDate.getOrElse(taxYearResolver.endOfCurrentTaxYear.toString().replace("-", ""))
      if (startDate != endDate) {
        val start = taxYearResolver.taxYearFor(LocalDate.parse(startDate, format))
        val end = taxYearResolver.taxYearFor(LocalDate.parse(endDate, format))
        val list = start until end + 1 toList;
        availedYears = availedYears ::: list
      }
    }
    Future { availedYears }
  }

  private def getEligibleTaxYearList(marriageDate: LocalDate)(implicit ec: ExecutionContext): Future[List[Int]] = {
    val marriageYear = taxYearResolver.taxYearFor(marriageDate)
    val currentYear = taxYearResolver.taxYearFor(LocalDate.now())
    val eligibleTaxYearList = (marriageYear < startTaxYear) match {
      case true => List.range(startTaxYear, currentYear + 1, 1).takeRight(5)
      case _    => List.range(marriageYear, currentYear + 1, 1).takeRight(5)
    }
    Future { eligibleTaxYearList }
  }

  private def getListOfEligibleTaxYears(transferorYears: List[Int], recipientYears: List[Int], eligibleYearsBasdOnDoM: List[Int])(implicit ec: ExecutionContext): Future[List[TaxYear]] = {
    var eligibleYears = List[TaxYear]()
    for (yr <- eligibleYearsBasdOnDoM) {
      if (!(transferorYears.contains(yr) || recipientYears.contains(yr))) {
        eligibleYears = convertToTaxYear(yr, taxYearResolver.currentTaxYear == yr) :: eligibleYears
      }
    }
    Future { eligibleYears }
  }

  private def convertToTaxYear(year: Int, currentTaxYear: Boolean): TaxYear =
    TaxYear(
      year = year,
      isCurrent = if (currentTaxYear) Some(true) else None)

}
