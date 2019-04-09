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

package services

import java.text.SimpleDateFormat
import java.util.Calendar

import config.ApplicationConfig._
import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import errors._
import metrics.Metrics
import models.{TaxYear => TaxYearModel, _}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}

object MarriageAllowanceService extends MarriageAllowanceService {
  override val dataConnector = MarriageAllowanceDataConnector
  override val emailConnector = EmailConnector
  override val metrics = Metrics
  override val startTaxYear = START_TAX_YEAR
  override val maSupportedYearsCount = MA_SUPPORTED_YEARS_COUNT
}

trait MarriageAllowanceService {

  val dataConnector: MarriageAllowanceDataConnector
  val emailConnector: EmailConnector
  val metrics: Metrics
  val startTaxYear: Int
  val maSupportedYearsCount: Int

  def currentTaxYear: Int = TaxYear.current.startYear

  def getRecipientRelationship(transferorNino: Nino, findRecipientRequest: FindRecipientRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(UserRecord, List[TaxYearModel])] = {
    for {
      recipientRecord <- getRecipientRecord(findRecipientRequest)
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
    val pickTemp = pickTemplate(isWelsh)(_,_)
    Future {
      taxYears.contains(currentTaxYear) match {
        case true if taxYears.size == 1 => pickTemp(EMAIL_APPLY_CURRENT_TAXYEAR_WELSH_TEMPLATE_ID, EMAIL_APPLY_CURRENT_TAXYEAR_TEMPLATE_ID)
        case true if taxYears.size > 1 => pickTemp(EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID, EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID)
        case _ => pickTemp(EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID, EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID)
      }
    }
  }

  private def pickTemplate(isWelsh: Boolean)(cyTemplate: String, enTemplate: String): String =
    isWelsh match {
      case true => cyTemplate
      case _ => enTemplate
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
            startDate = Some(TaxYear(taxYear).starts.toString()),
            endDate = Some(TaxYear(taxYear).finishes.toString())))
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
              case error =>
                throw MultiYearCreateRelationshipError(error)
            }
        }
    }

  private def isCurrentTaxYear(taxYear: Int): Boolean = {
    currentTaxYear == taxYear
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
    val (emailTemplateId, startDate, endDate) = getEmailTemplateId(updateRelationshipRequestHolder.request.relationship, updateRelationshipRequestHolder.notification.role,
        updateRelationshipRequestHolder.notification.welsh, updateRelationshipRequestHolder.notification.isRetrospective)
    val emailParameters: Map[String, String] = Map("full_name" -> updateRelationshipRequestHolder.notification.full_name, "startDate" -> startDate, "endDate" -> endDate)
    Future.successful(SendEmailRequest(templateId = emailTemplateId, to = emailRecipients, parameters = emailParameters, force = false))
  }

  private def getEmailTemplateId(relationship: DesRelationshipInformation, role: String, isWelsh: Boolean, isRetrospective: Boolean): (String, String, String) = {
    val pickTemp = pickTemplate(isWelsh)(_,_)
    val (startDate, endDate) = isWelsh match {
      case true => (START_DATE_CY, END_DATE_CY)
      case _ => (START_DATE, END_DATE)
    }

    val startDateNextYear = startDate + (currentTaxYear + 1)
    val endDateNextYear = endDate + (currentTaxYear + 1)
    val startDateCurrYear = startDate + currentTaxYear
    val endDateCurrYear = endDate + currentTaxYear

    (relationship.relationshipEndReason, role) match {
      case (REASON_CANCEL, _) =>
        val template = pickTemp(EMAIL_UPDATE_CANCEL_WELSH_TEMPLATE_ID, EMAIL_UPDATE_CANCEL_TEMPLATE_ID)
        (template, startDateNextYear, endDateNextYear)
      case (REASON_REJECT, ROLE_RECIPIENT) =>
        if (!isRetrospective) (pickTemp(EMAIL_UPDATE_REJECT_WELSH_TEMPLATE_ID, EMAIL_UPDATE_REJECT_TEMPLATE_ID), "", "")
        else (pickTemp(EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR_WELSH, EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR), "", "")
      case (REASON_DIVORCE, ROLE_TRANSFEROR) =>
        if (relationship.actualEndDate == getCurrentElseRetroYearDateInFormat(isCurrent = true))
          (pickTemp(EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR_WELSH, EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR), startDateNextYear, endDateNextYear)
        else if (relationship.actualEndDate == getCurrentElseRetroYearDateInFormat(isCurrent = false))
          (pickTemp(EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_WELSH_TEMPLATE_ID, EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_TEMPLATE_ID),startDateCurrYear, "")
        else (pickTemp(EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR_WELSH, EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR), startDateCurrYear, endDateCurrYear)
      case (REASON_DIVORCE, ROLE_RECIPIENT) =>
        if (relationship.actualEndDate == getCurrentElseRetroYearDateInFormat(isCurrent = true))
          (pickTemp(EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_WELSH_TEMPLATE_ID, EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_TEMPLATE_ID), startDateNextYear, endDateNextYear)
        else (pickTemp(EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR_WELSH, EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR), "", endDateCurrYear)
    }
  }

  private def getCurrentElseRetroYearDateInFormat(isCurrent: Boolean): String = {
    val DATE_FORMAT = "yyyyMMdd"
    val sdf = new SimpleDateFormat(DATE_FORMAT)
    val currentdate = Calendar.getInstance()
    isCurrent match {
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
        Logger.warn("Cannot send email", error.getMessage)
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
        httpResponse.status match {
          case 200  => metrics.incrementSuccessCounter(ApiType.UpdateRelationship)
                      Future.successful(Unit)
          case 400  => throw RecipientDeceasedError("Service returned response with 400 - recipient deceased")
          case _    => throw UpdateRelationshipError("An unexpected error has occured while updating the relationship")
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
    val format = DateTimeFormat.forPattern("yyyyMMdd")
    val relationships = relationshipRecordWrapper.relationshipRecordList
    var availedYears: List[Int] = List()
    for (record <- relationships) {
      val startDate = record.participant1StartDate
      val endDate = record.participant1EndDate.getOrElse(TaxYear.current.finishes.toString.replace("-", ""))
      if (startDate != endDate) {
        val start: Int = TaxYear.taxYearFor(LocalDate.parse(startDate, format)).startYear
        val end: Int = TaxYear.taxYearFor(LocalDate.parse(endDate, format)).startYear
        val list = start until end + 1 toList;
        availedYears = availedYears ::: list
      }
    }
    Future { availedYears }
  }

  private def getEligibleTaxYearList(marriageDate: LocalDate)(implicit ec: ExecutionContext): Future[List[Int]] = {
    val marriageYear = TaxYear.taxYearFor(marriageDate).startYear
    val currentYear = currentTaxYear
    val eligibleTaxYearList = (marriageYear < startTaxYear) match {
      case true => List.range(startTaxYear, currentYear + 1, 1).takeRight(maSupportedYearsCount)
      case _    => List.range(marriageYear, currentYear + 1, 1).takeRight(maSupportedYearsCount)
    }
    Future { eligibleTaxYearList }
  }

  private def getListOfEligibleTaxYears(transferorYears: List[Int], recipientYears: List[Int], eligibleYearsBasdOnDoM: List[Int])(implicit ec: ExecutionContext): Future[List[TaxYearModel]] = {
    var eligibleYears = List[TaxYearModel]()
    for (yr <- eligibleYearsBasdOnDoM) {
      if (!(transferorYears.contains(yr) || recipientYears.contains(yr))) {
        eligibleYears = convertToTaxYear(yr, currentTaxYear == yr) :: eligibleYears
      }
    }
    Future { eligibleYears }
  }

  private def convertToTaxYear(year: Int, currentTaxYear: Boolean): TaxYearModel =
    TaxYearModel(
      year = year,
      isCurrent = if (currentTaxYear) Some(true) else None)

}
