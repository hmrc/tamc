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

package controllers

import models.MultiYearDesCreateRelationshipRequest
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, contentAsString}
import test_utils.TestData.Cids
import test_utils.TestData
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class MultiYearSpec extends UnitSpec with GuiceOneAppPerSuite {

  val controller = app.injector.instanceOf[MarriageAllowanceController]


  "Calling Multi Year create relationship" should {

    "return OK if data is correct for current tax year" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = app.injector.instanceOf[MarriageAllowanceController]


      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK
    }

    "return OK if data is correct for retrospective year 2015/16, if current tax year is set in the future (1st Jan 2017)" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK
    }

    "return OK if data is correct for retrospective tax year" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK
    }

    "return OK if data is correct for multiple years" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015, 2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK
    }


    "return RELATION-MIGHT-BE-CREATED if data is in conflict state (409) for multiple years" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val testData = s"""{"request":{"transferor_cid":${Cids.cidConflict}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015, 2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val response = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(response) shouldBe OK
      val json = Json.parse(contentAsString(response)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:RELATION-MIGHT-BE-CREATED"
    }

    "return RELATION-MIGHT-BE-CREATED if request results in LTM000503 (503) for multiple years" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val testData = s"""{"request":{"transferor_cid":${Cids.cidServiceUnavailable}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015, 2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val response = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(response) shouldBe OK
      val json = Json.parse(contentAsString(response)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:RELATION-MIGHT-BE-CREATED"
    }

  }

  "Create Multi Year request model" should {
    "transform to correct DES request" in {

      val model = MultiYearDesCreateRelationshipRequest(
        recipientCid = "1111",
        recipientTimestamp = "2222",
        transferorCid = "3333",
        transferorTimestamp = "4444",
        startDate = Some("2015-01-01"),
        endDate = Some("2016-02-02"))

      val expextedResult =
        """
{
    "CID1": "1111",
    "CID1Timestamp": "2222",
    "CID2": "3333",
    "CID2Timestamp": "4444",
    "startDate" : "2015-01-01",
    "endDate" : "2016-02-02"
}"""

      Json.toJson(model) shouldBe Json.parse(expextedResult)
    }
  }
}