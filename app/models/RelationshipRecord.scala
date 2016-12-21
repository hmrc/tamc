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

package models

import play.api.libs.json.Format
import play.api.libs.json._
import config.ApplicationConfig
import play.api.Logger

case class RelationshipRecordWrapper(
  relationshipRecordList: Seq[RelationshipRecord],
  userRecord: Option[UserRecord] = None)

case class RelationshipRecord(participant: String, creationTimestamp: String, participant1StartDate: String, relationshipEndReason: Option[String] = None, participant1EndDate: Option[String] = None, otherParticipantInstanceIdentifier: String, otherParticipantUpdateTimestamp: String) {
  def this(participant: Int, creationTimestamp: String, participant1StartDate: String, relationshipEndReason: Option[String], participant1EndDate: Option[String], otherParticipantInstanceIdentifier: String, otherParticipantUpdateTimestamp: String) =
    this(if (participant == 1) ApplicationConfig.ROLE_RECIPIENT else ApplicationConfig.ROLE_TRANSFEROR, creationTimestamp, participant1StartDate, relationshipEndReason, participant1EndDate, otherParticipantInstanceIdentifier, otherParticipantUpdateTimestamp)
}

object RelationshipRecord {

  implicit object RelationshipRecordFormat extends Format[RelationshipRecord] {
    def reads(json: JsValue) = JsSuccess(new RelationshipRecord(

      (json \ "participant").as[Int],
      (json \ "creationTimestamp").as[String],
      (json \ "participant1StartDate").as[String],
      (json \ "relationshipEndReason").asOpt[String],
      (json \ "participant1EndDate").asOpt[String],
      (json \ "otherParticipantInstanceIdentifier").as[String],
      (json \ "otherParticipantUpdateTimestamp").as[String]))

    def writes(relatisoshipRecord: RelationshipRecord) = Json.obj(

      "participant" -> relatisoshipRecord.participant,
      "creationTimestamp" -> relatisoshipRecord.creationTimestamp,
      "participant1StartDate" -> relatisoshipRecord.participant1StartDate,
      "relationshipEndReason" -> transformEndReasonCodeDesc(relatisoshipRecord.relationshipEndReason),
      "participant1EndDate" -> relatisoshipRecord.participant1EndDate,
      "otherParticipantInstanceIdentifier" -> relatisoshipRecord.otherParticipantInstanceIdentifier,
      "otherParticipantUpdateTimestamp" -> relatisoshipRecord.otherParticipantUpdateTimestamp)
  }

  private def transformEndReasonCodeDesc(endReasonCode: Option[String]): Option[String] = {
    endReasonCode match {
      case None => None
      case Some("Death (either participant)") => Some("DEATH")
      case Some("Divorce/Separation") => Some("DIVORCE")
      case Some("Relationship Type specific - for future use") => Some("DEFAULT")
      case Some("Invalid Participant") => Some("INVALID_PARTICIPANT")
      case Some("Ended by Participant 2") => Some("CANCELLED")
      case Some("Ended by Participant 1") => Some("REJECTED")
      case Some("Ended by HMRC") => Some("HMRC")
      case Some("Closed by mutual consent of participants") => Some("CLOSED")
      case Some("Merger") => Some("MERGER")
      case Some("Retrospective") => Some("RETROSPECTIVE")
      case Some("System Closure") => Some("SYSTEM")
      case unknown =>
        Logger.warn(s"Unexpected reason code :'${unknown}'")
        Some("DEFAULT")
    }
  }
}

object RelationshipRecordWrapper {

  implicit object RelationshipRecordListFormat extends Format[RelationshipRecordWrapper] {
    def reads(json: JsValue) = JsSuccess(RelationshipRecordWrapper(
      (json \ "relationships").as[Seq[RelationshipRecord]]))
    def writes(relatisoshipRecordWrapper: RelationshipRecordWrapper) = Json.obj("relationships" -> relatisoshipRecordWrapper.relationshipRecordList,
      "userRecord" -> relatisoshipRecordWrapper.userRecord)
  }
}
