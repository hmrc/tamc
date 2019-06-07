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

package models

import play.api.libs.json._

sealed trait RelationshipEndReason {
  def name: String
}

case object DEATH extends RelationshipEndReason {
  val name = "DEATH"
}

case object DIVORCE extends RelationshipEndReason {
  val name = "DIVORCE"
}

case object DEFAULT extends RelationshipEndReason {
  val name = "DEFAULT"
}

case object INVALID_PARTICIPANT extends RelationshipEndReason {
  val name = "INVALID_PARTICIPANT"
}

case object CANCELLED extends RelationshipEndReason {
  val name = "CANCELLED"
}

case object REJECTED extends RelationshipEndReason {
  val name = "REJECTED"
}

case object HMRC extends RelationshipEndReason {
  val name = "HMRC"
}

case object CLOSED extends RelationshipEndReason {
  val name = "CLOSED"
}

case object MERGER extends RelationshipEndReason {
  val name = "MERGER"
}

case object RETROSPECTIVE extends RelationshipEndReason {
  val name = "RETROSPECTIVE"
}

case object SYSTEM extends RelationshipEndReason {
  val name = "SYSTEM"
}

case object ACTIVE extends RelationshipEndReason {
  val name = "ACTIVE"
}

object RelationshipEndReason {

  implicit val RelationshipEndReasonFormat: Format[RelationshipEndReason] = new Format[RelationshipEndReason] {
    def reads(json: JsValue): JsResult[RelationshipEndReason] = JsSuccess(RelationshipEndReason(json.as[String]).getOrElse(
      throw new IllegalArgumentException("Invalid relationship end reason type")))

    def writes(myEnum: RelationshipEndReason): JsValue = JsString(myEnum.name)
  }

  def apply(name: String): Option[RelationshipEndReason] = name match {
    case DEATH.name => Some(DEATH)
    case DIVORCE.name => Some(DIVORCE)
    case DEFAULT.name => Some(DEFAULT)
    case INVALID_PARTICIPANT.name => Some(INVALID_PARTICIPANT)
    case CANCELLED.name => Some(CANCELLED)
    case REJECTED.name => Some(REJECTED)
    case HMRC.name => Some(HMRC)
    case CLOSED.name => Some(CLOSED)
    case MERGER.name => Some(MERGER)
    case RETROSPECTIVE.name => Some(RETROSPECTIVE)
    case SYSTEM.name => Some(SYSTEM)
    case ACTIVE.name => Some(ACTIVE)
    case _ => None
  }

}
