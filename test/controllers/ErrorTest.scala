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

class ErrorTest extends UnitSpec with TestUtility {
  "Checking user record" should {

    "return BadRequest if there is an error while finding cid for recipient" in new WithApplication(FakeApplication()) {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino = Nino(transferorNinoObject.nino)

      val recipient = TestData.Recipients.recHasNoAllowanceNoCid
      val recipientNino = recipient.citizen.nino
      val recipientCid = recipient.citizen.cid.cid
      val recipientGender = recipient.gender

      val controller = makeFakeController()
      val testData = s"""{"name":"foo","lastName":"bar", "nino":"${recipientNino}", "gender":"${recipientGender}"}"""
      val request = FakeRequest().withBody(Json.parse(testData))

      val result = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:RECIPIENT-NOT-FOUND"
    }

    "return BadRequest if gender is invalid" in new WithApplication(FakeApplication()) {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino = Nino(transferorNinoObject.nino)

      val recipient = TestData.Recipients.recHasNoAllowance
      val recipientNino = recipient.citizen.nino
      val recipientCid = recipient.citizen.cid.cid

      val controller = makeFakeController()
      val testData = s"""{"name":"fgh","lastName":"asd", "nino":"${recipientNino}", "gender":"123"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.getRecipientRelationship(transferorNino)(request)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
