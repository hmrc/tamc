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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.WithApplication
import test_utils.TestUtility
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec
import test_utils.TestData
import models.MultiYearDesCreateRelationshipRequest

class CustomJson extends UnitSpec with TestUtility {
  "MultiYearDesCreateRelationshipRequest be properly transformed to JSON" should {
    "if dates are not available" in {
      Json.toJson(MultiYearDesCreateRelationshipRequest("a","b","c","d", None, None)).toString() shouldBe """{"CID1":"a","CID1Timestamp":"b","CID2":"c","CID2Timestamp":"d"}"""
    }

    "if dates are available" in {
      Json.toJson(MultiYearDesCreateRelationshipRequest("a","b","c","d", Some("f"), Some("g"))).toString() shouldBe """{"CID1":"a","CID1Timestamp":"b","CID2":"c","CID2Timestamp":"d","startDate":"f","endDate":"g"}"""
    }
  }
}
