package test_utils

import models.{ResponseStatus, UserRecord}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}

case class TestRelationshipRecordWrapper(relationshipRecordList: Seq[TestRelationshipRecord], userRecord: UserRecord)
case class TestRelationshipRecord(participant: String, creationTimestamp: String, participant1StartDate: String, relationshipEndReason: Option[String] = None, participant1EndDate: Option[String] = None, otherParticipantInstanceIdentifier: String, otherParticipantUpdateTimestamp: String)
case class TestRelationshipRecordStatusWrapper(relationship_record: TestRelationshipRecordWrapper = null, status: ResponseStatus)

object TestRelationshipRecord {

  implicit object TestRelationshipRecordFormat extends Format[TestRelationshipRecord] {
    def reads(json: JsValue) = JsSuccess(new TestRelationshipRecord(

      (json \ "participant").as[String],
      (json \ "creationTimestamp").as[String],
      (json \ "participant1StartDate").as[String],
      (json \ "relationshipEndReason").as[Option[String]],
      (json \ "participant1EndDate").as[Option[String]],
      (json \ "otherParticipantInstanceIdentifier").as[String],
      (json \ "otherParticipantUpdateTimestamp").as[String]))

    def writes(relatisoshipRecord: TestRelationshipRecord) = Json.obj(

      "participant" -> relatisoshipRecord.participant,
      "creationTimestamp" -> relatisoshipRecord.creationTimestamp,
      "participant1StartDate" -> relatisoshipRecord.participant1StartDate,
      "relationshipEndReason" -> relatisoshipRecord.relationshipEndReason,
      "participant1EndDate" -> relatisoshipRecord.participant1EndDate,
      "otherParticipantInstanceIdentifier" -> relatisoshipRecord.otherParticipantInstanceIdentifier,
      "otherParticipantUpdateTimestamp" -> relatisoshipRecord.otherParticipantUpdateTimestamp)
  }
}

object TestRelationshipRecordWrapper {

  implicit object RelationshipRecordListFormat extends Format[TestRelationshipRecordWrapper] {
    def reads(json: JsValue) = JsSuccess(TestRelationshipRecordWrapper(
      (json \ "relationships").as[Seq[TestRelationshipRecord]],
      (json \ "userRecord").as[UserRecord]))
    def writes(relatisoshipRecordWrapper: TestRelationshipRecordWrapper) = Json.obj("relationships" -> relatisoshipRecordWrapper.relationshipRecordList,
      "userRecord" -> relatisoshipRecordWrapper.userRecord)
  }
}

object TestRelationshipRecordStatusWrapper {
  implicit val formats = Json.format[TestRelationshipRecordStatusWrapper]
}
