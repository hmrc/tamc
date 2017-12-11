/*
 * Copyright 2017 HM Revenue & Customs
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
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout}
import test_utils.TestData.Cids
import test_utils.{TestData, TestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, ConflictException, Upstream4xxResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class MultiYearTest extends UnitSpec with TestUtility with OneAppPerSuite {

  override implicit lazy val app: Application = fakeApplication

  "Calling Multi Year create relationship" should {
   /* "return OK if data is correct for current tax year" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController()
      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK

      val postRequests = controller.debugData.httpPostCallsToTest

      postRequests.size shouldBe 1

      val firstRequest = postRequests.head

      firstRequest.url shouldBe s"POST-foo/marriage-allowance/02.00.00/citizen/${recipientCid}/relationship/active"

      val first: MultiYearDesCreateRelationshipRequest = firstRequest.body.asInstanceOf[MultiYearDesCreateRelationshipRequest]

      first.recipientCid shouldBe recipientCid.toString()
      first.transferorCid shouldBe transferorCid.toString()

      first.recipientTimestamp shouldBe recipientTs
      first.transferorTimestamp shouldBe transferorTs

      first.startDate shouldBe None
      first.endDate shouldBe None
    }

    "return OK if data is correct for retrospective year 2015/16, if current tax year is set in the future (1st Jan 2017)" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController(testingTime = new DateTime(2017, 1, 1, 0, 0, DateTimeZone.forID("Europe/London")))
      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK

      val postRequests = controller.debugData.httpPostCallsToTest

      postRequests.size shouldBe 1

      val firstRequest = postRequests.head

      firstRequest.url shouldBe s"POST-foo/marriage-allowance/02.00.00/citizen/${recipientCid}/relationship/retrospective"

      val first: MultiYearDesCreateRelationshipRequest = firstRequest.body.asInstanceOf[MultiYearDesCreateRelationshipRequest]

      first.recipientCid shouldBe recipientCid.toString()
      first.transferorCid shouldBe transferorCid.toString()

      first.recipientTimestamp shouldBe recipientTs
      first.transferorTimestamp shouldBe transferorTs

      first.startDate shouldBe Some("2015-04-06")
      first.endDate shouldBe Some("2016-04-05")
    }

    "return OK if data is correct for retrospective tax year" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController()
      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK

      val postRequests = controller.debugData.httpPostCallsToTest

      postRequests.size shouldBe 1

      val firstRequest = postRequests.head

      firstRequest.url shouldBe s"POST-foo/marriage-allowance/02.00.00/citizen/${recipientCid}/relationship/retrospective"

      val first: MultiYearDesCreateRelationshipRequest = firstRequest.body.asInstanceOf[MultiYearDesCreateRelationshipRequest]

      first.recipientCid shouldBe recipientCid.toString()
      first.transferorCid shouldBe transferorCid.toString()

      first.recipientTimestamp shouldBe recipientTs
      first.transferorTimestamp shouldBe transferorTs

      first.startDate shouldBe Some("2014-04-06")
      first.endDate shouldBe Some("2015-04-05")
    }

    "return OK if data is correct for multiple years" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController()
      val testData = s"""{"request":{"transferor_cid":${transferorCid}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015, 2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(result) shouldBe OK

      val postRequests = controller.debugData.httpPostCallsToTest

      postRequests.size shouldBe 2

      val firstRequest = postRequests.head
      val secondRequest = postRequests.tail.head

      firstRequest.url shouldBe s"POST-foo/marriage-allowance/02.00.00/citizen/${recipientCid}/relationship/active"
      secondRequest.url shouldBe s"POST-foo/marriage-allowance/02.00.00/citizen/${recipientCid}/relationship/retrospective"

      val first: MultiYearDesCreateRelationshipRequest = firstRequest.body.asInstanceOf[MultiYearDesCreateRelationshipRequest]
      val second: MultiYearDesCreateRelationshipRequest = secondRequest.body.asInstanceOf[MultiYearDesCreateRelationshipRequest]

      first.recipientCid shouldBe recipientCid.toString()
      first.transferorCid shouldBe transferorCid.toString()

      second.recipientCid shouldBe recipientCid.toString()
      second.transferorCid shouldBe transferorCid.toString()

      first.recipientTimestamp shouldBe recipientTs.toString()
      first.transferorTimestamp shouldBe transferorTs.toString()

      second.recipientTimestamp shouldBe TestData.MultiYearCreate.happyScenarioStep2.recipient.timestamp
      second.transferorTimestamp shouldBe TestData.MultiYearCreate.happyScenarioStep2.transferor.timestamp

      first.startDate shouldBe None
      first.endDate shouldBe None

      second.startDate shouldBe Some("2014-04-06")
      second.endDate shouldBe Some("2015-04-05")
    }*/


    "return RELATION-MIGHT-BE-CREATED if data is in conflict state (409) for multiple years" in {

      val testInput = TestData.MultiYearCreate.happyScenarioStep1
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController(isErrorController = true)
      val testData = s"""{"request":{"transferor_cid":${Cids.cidConflict}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015, 2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val response = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(response) shouldBe OK
      val json = Json.parse(contentAsString(response))
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

      val controller = makeFakeController(isErrorController = true)
      val testData = s"""{"request":{"transferor_cid":${Cids.cidServiceUnavailable}, "transferor_timestamp": "${transferorTs}", "recipient_cid":${recipientCid}, "recipient_timestamp":"${recipientTs}", "taxYears":[2015, 2014]}, "notification":{"full_name":"foo bar", "email":"example@example.com", "welsh":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val response = controller.createMultiYearRelationship(transferorNino, "GDS")(request)
      status(response) shouldBe OK
      val json = Json.parse(contentAsString(response))
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
