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

import models.{FindRecipientRequest, Gender}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK, contentAsString}
import services.MarriageAllowanceService
import test_utils.TestData
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class ErrorSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  val mockMarriageAllowanceService: MarriageAllowanceService = mock[MarriageAllowanceService]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[MarriageAllowanceService].toInstance(mockMarriageAllowanceService)
    ).build()

  val controller: MarriageAllowanceController = app.injector.instanceOf[MarriageAllowanceController]

  "Checking user record" should {

    //TODO this needs investigating
    "return Recipient not found if there is an error while finding cid for recipient" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino = Nino(transferorNinoObject.nino)

      val recipient = TestData.Recipients.recHasNoAllowanceNoCid
      val recipientNino = recipient.citizen.nino
      val recipientGender = recipient.gender

      val testData = s"""{"name":"foo","lastName":"bar", "nino":"${recipientNino}", "gender":"${recipientGender}"}"""
      val request = FakeRequest().withBody(Json.parse(testData))

      val result = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:RECIPIENT-NOT-FOUND"
    }

    "when transferor deceased" should {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP6A)
      val transferorNino = Nino(transferorNinoObject.nino)

      "return transferor deceased BadRequest when recipient is good (has allowance)" in {
        val recipient = TestData.Recipients.recHasAllowance
        val recipientNino = recipient.citizen.nino
        val recipientGender = recipient.gender

        val testData = s"""{"name":"rty","lastName":"qwe", "nino":"${recipientNino}", "gender":"${recipientGender}"}"""
        val request = FakeRequest().withBody(Json.parse(testData))

        val result = controller.getRecipientRelationship(transferorNino)(request)
        status(result) shouldBe OK

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }

      "return transferor deceased BadRequest when recipient has allowance and space in name" in {

        val recipient = TestData.Recipients.recHasAllowanceAndSpaceInName
        val recipientNino = recipient.citizen.nino
        val recipientGender = recipient.gender

        val testData = s"""{"name":"rty","lastName":"qwe abc", "nino":"${recipientNino}", "gender":"${recipientGender}"}"""
        val request = FakeRequest().withBody(Json.parse(testData))

        val result = controller.getRecipientRelationship(transferorNino)(request)
        status(result) shouldBe OK

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }

      "return transferor deceased BadRequest when recipient has no allowance" in {

        val recipient = TestData.Recipients.recHasNoAllowance
        val recipientNino = recipient.citizen.nino
        val recipientGender = recipient.gender

        val testData = s"""{"name":"fgh","lastName":"asd", "nino":"${recipientNino}", "gender":"${recipientGender}"}"""
        val request = FakeRequest().withBody(Json.parse(testData))

        val result = controller.getRecipientRelationship(transferorNino)(request)
        status(result) shouldBe OK

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }

      //TODO this requires investigating
      "return transferor deceased BadRequest when recipient not found" ignore {

        val testData = s"""{"name":"abc","lastName":"def", "nino":"AB242424B", "gender":"M"}"""
        val request = FakeRequest().withBody(Json.parse(testData))

        val result = controller.getRecipientRelationship(transferorNino)(request)
        status(result) shouldBe OK

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }
    }

    "return BadRequest if gender is invalid" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino = Nino(transferorNinoObject.nino)

      val recipient = TestData.Recipients.recHasNoAllowance
      val recipientNino = recipient.citizen.nino
      val recipientCid = recipient.citizen.cid.cid

      val testData = s"""{"name":"fgh","lastName":"asd", "nino":"${recipientNino}", "gender":"123"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.getRecipientRelationship(transferorNino)(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return bad request should be handled" in {

      val request = FakeRequest()

      val testData = TestData.Lists.badRequest
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:BAD-REQUEST"
    }

    "return NotFound should be handled" in {

      val request = FakeRequest()

      val testData = TestData.Lists.citizenNotFound
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:CITIZEN-NOT-FOUND"
    }

    "return InternalServerException should be handled" in {

      val request = FakeRequest()

      val testData = TestData.Lists.serverError
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "ERROR:500"
    }

    "return Service unavailable should be handled" in {

      val request = FakeRequest()

      val testData = TestData.Lists.serviceUnavailable
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "ERROR:503"
    }

  }

  "Update Relationship " should {

    "handle Bad request and show transferor is deceased" in {

      val testInput = TestData.Updates.badRequest
      val recipientNino = Nino(testInput.transferor.nino)
      val recipientCid = testInput.transferor.cid.cid
      val transferorNino = Nino(testInput.recipient.nino)
      val transferorCid = testInput.recipient.cid.cid
      val recipientTs = testInput.transferor.timestamp.toString()
      val transferorTs = testInput.recipient.timestamp.toString()

      val testData = s"""{"request":{"participant1":{"instanceIdentifier":"${recipientCid}","updateTimestamp":"${recipientTs}"},"participant2":{"updateTimestamp":"${transferorTs}"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Cancelled by Transferor","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:BAD-REQUEST"
    }

    "handle Update relationship error" in {

      val testInput = TestData.Updates.citizenNotFound
      val recipientNino = Nino(testInput.transferor.nino)
      val recipientCid = testInput.transferor.cid.cid
      val transferorNino = Nino(testInput.recipient.nino)
      val transferorCid = testInput.recipient.cid.cid
      val recipientTs = testInput.transferor.timestamp.toString()
      val transferorTs = testInput.recipient.timestamp.toString()

      val testData = s"""{"request":{"participant1":{"instanceIdentifier":"${recipientCid}","updateTimestamp":"${recipientTs}"},"participant2":{"updateTimestamp":"${transferorTs}"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Cancelled by Transferor","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:CANNOT-UPDATE-RELATIONSHIP"
    }
  }
}