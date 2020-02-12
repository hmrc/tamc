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

import errors.ErrorResponseStatus.RECIPIENT_NOT_FOUND
import errors.TooManyRequestsError
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.Helpers.{OK, contentAsJson, contentAsString, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import services.MarriageAllowanceService
import test_utils._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class MarriageAllowanceControllerTest extends UnitSpec with TestUtility with GuiceOneAppPerSuite with MockitoSugar {

  lazy val controller = new MarriageAllowanceController {
    override val marriageAllowanceService = mock[MarriageAllowanceService]
    override val authAction = FakeAuthAction
  }

  trait Setup {
    val generatedNino = new Generator().nextNino
    val findRecipientRequest = FindRecipientRequest(name = "testName", lastName = "lastName", gender = Gender("M"), generatedNino)
    val json = Json.toJson(findRecipientRequest)
    val fakeRequest = FakeRequest("POST", "/", FakeHeaders(), Json.toJson(json))
  }

  "Marriage Allowance Controller" should {

    "return OK when a valid UserRecord and TaxYearModel are received" in new Setup {

        val userRecord = UserRecord(cid = 123456789, timestamp = "20200116155359011123")
        val taxYearList = List(TaxYear(2019))

        when(controller.marriageAllowanceService.getRecipientRelationship(ArgumentMatchers.eq(generatedNino), ArgumentMatchers.eq(findRecipientRequest))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right((userRecord, taxYearList))))

        val result = controller.getRecipientRelationship(generatedNino)(fakeRequest)

        val expectedResponse = Json.toJson(GetRelationshipResponse(
          user_record = Some(userRecord),
          availableYears = Some(taxYearList),
          status = ResponseStatus(status_code = "OK")))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedResponse)
      }
    }

    "return a RecipientNotFound error after receiving a DataRetrievalError" in  new Setup {

      when(controller.marriageAllowanceService.getRecipientRelationship(ArgumentMatchers.eq(generatedNino), ArgumentMatchers.eq(findRecipientRequest))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Left(TooManyRequestsError)))

      val result = controller.getRecipientRelationship(generatedNino)(fakeRequest)

      val expectedResponse = GetRelationshipResponse(
        status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND))

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)

    }




  "Calling hasMarriageAllowance for Recipient" should {

    "return OK if cid is founds" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino = Nino(transferorNinoObject.nino)

      val recipient = TestData.Recipients.recHasAllowance
      val recipientNino = recipient.citizen.nino
      val recipientCid = recipient.citizen.cid.cid
      val recipientGender = recipient.gender

      val controller = makeFakeController()
      val testData = s"""{"name":"rty","lastName":"qwe", "nino":"${recipientNino}", "gender":"${recipientGender}", "dateOfMarriage":"01/01/2015"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "user_record" \ "has_allowance").asOpt[Boolean] shouldBe None
      (json \ "status" \ "status_code").as[String] shouldBe "OK"

      controller.debugData.findRecipientNinoToTest shouldBe Some(Nino(recipientNino))
      controller.debugData.findRecipientNinoToTestCount shouldBe 1

      val findRecipientCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${recipientNino}/check?surname=qwe&forename1=rty&gender=M",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val checkHistoricAllowanceRelationshipCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${recipientCid}/relationships?includeHistoric=true",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val findTransferorCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${transferorNino.nino}",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val checkTransferorHistoricAllowanceRelationshipCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${transferorNinoObject.cid.cid}/relationships?includeHistoric=true",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      controller.debugData.httpGetCallsToTest shouldBe List(
        findRecipientCall,
        findTransferorCall,
        checkHistoricAllowanceRelationshipCall,
        checkTransferorHistoricAllowanceRelationshipCall)
    }

    "return OK if cid is found and allowance relationship exists and surname has space in between" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino = Nino(transferorNinoObject.nino)

      val recipient = TestData.Recipients.recHasAllowance
      val recipientNino = recipient.citizen.nino
      val recipientCid = recipient.citizen.cid.cid
      val recipientGender = recipient.gender

      val controller = makeFakeController()
      val testData = s"""{"name":"rty","lastName":"qwe abc", "nino":"${recipientNino}", "gender":"${recipientGender}", "dateOfMarriage":"01/01/2015"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "user_record" \ "has_allowance").asOpt[Boolean] shouldBe None
      (json \ "status" \ "status_code").as[String] shouldBe "OK"

      controller.debugData.findRecipientNinoToTest shouldBe Some(Nino(recipientNino))
      controller.debugData.findRecipientNinoToTestCount shouldBe 1

      val findRecipientCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${recipientNino}/check?surname=qwe+abc&forename1=rty&gender=M",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val checkHistoricAllowanceRelationshipCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${recipientCid}/relationships?includeHistoric=true",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val findTransferorCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${transferorNino.nino}",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val checkTransferorHistoricAllowanceRelationshipCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${transferorNinoObject.cid.cid}/relationships?includeHistoric=true",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      controller.debugData.httpGetCallsToTest shouldBe List(
        findRecipientCall,
        findTransferorCall,
        checkHistoricAllowanceRelationshipCall,
        checkTransferorHistoricAllowanceRelationshipCall)
    }

    "return false if cid is found and allowance relationship does not exist" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino = Nino(transferorNinoObject.nino)

      val recipient = TestData.Recipients.recHasNoAllowance
      val recipientNino = recipient.citizen.nino
      val recipientCid = recipient.citizen.cid.cid
      val recipientGender = recipient.gender

      val controller = makeFakeController()
      val testData = s"""{"name":"fgh","lastName":"asd", "nino":"${recipientNino}", "gender":"${recipientGender}", "dateOfMarriage":"01/01/2015"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "user_record" \ "has_allowance").asOpt[Boolean] shouldBe None
      (json \ "status" \ "status_code").as[String] shouldBe "OK"

      controller.debugData.findRecipientNinoToTest shouldBe Some(Nino(recipientNino))
      controller.debugData.findRecipientNinoToTestCount shouldBe 1

      val findRecipientCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${recipientNino}/check?surname=asd&forename1=fgh&gender=F",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val checkHistoricAllowanceRelationshipCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${recipientCid}/relationships?includeHistoric=true",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val findTransferorCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${transferorNino.nino}",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      val checkTransferorHistoricAllowanceRelationshipCall = HttpGETCallWithHeaders(
        url = s"GET-foo/marriage-allowance/citizen/${transferorNinoObject.cid.cid}/relationships?includeHistoric=true",
        env = Seq(("Environment" -> "test-environment")),
        bearerToken = Some(Authorization("test-bearer-token")))

      controller.debugData.httpGetCallsToTest shouldBe List(
        findRecipientCall,
        findTransferorCall,
        checkHistoricAllowanceRelationshipCall,
        checkTransferorHistoricAllowanceRelationshipCall)
    }
  }

  "Calling list relationship for logged in person" should {

    "check if list contains one active and one historic relationship" in {
      val controller = makeFakeController()
      val request = FakeRequest()

      val testData = TestData.Lists.oneActiveOneHistoric
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid
      val testTs = testData.user.timestamp.toString()

      val participiant0 = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts = participiant0.partner.timestamp.toString()

      val participiant1 = testData.counterparties(1)
      val participiant1Cid: String = participiant1.partner.cid.cid.toString
      val participiant1Ts = participiant1.partner.timestamp.toString()

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]

      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 2
      var expectedOutputMap = Map("participant" -> "Recipient", "creationTimestamp" -> "20150531235901", "participant1StartDate" -> "20011230",
        "relationshipEndReason" -> None, "participant1EndDate" -> None, "otherParticipantInstanceIdentifier" -> participiant0Cid,
        "otherParticipantUpdateTimestamp" -> participiant0Ts)

      testStubDataFromAPI(list(0), expectedOutputMap)

      expectedOutputMap = Map("participant" -> "Recipient", "creationTimestamp" -> "20150531235901", "participant1StartDate" -> "20011230",
        "relationshipEndReason" -> "DEATH", "participant1EndDate" -> "20101230", "otherParticipantInstanceIdentifier" -> participiant1Cid,
        "otherParticipantUpdateTimestamp" -> participiant1Ts)

      testStubDataFromAPI(list(1), expectedOutputMap)

      val userRecord = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)
    }

    "check for no relationship" in {
      val controller = makeFakeController()
      val request = FakeRequest()

      val testData = TestData.Lists.noRelations
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid
      val testTs = testData.user.timestamp.toString()

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 0

      val userRecord = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)
    }

    "check for historic relationship" in {
      val controller = makeFakeController()
      val request = FakeRequest()

      val testData = TestData.Lists.oneHistoric
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid
      val testTs = testData.user.timestamp.toString()

      val participiant0 = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts = participiant0.partner.timestamp.toString()

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 1
      val expectedOutputMap = Map("participant" -> "Recipient", "creationTimestamp" -> "20150531235901", "participant1StartDate" -> "20011230",
        "relationshipEndReason" -> "CANCELLED", "participant1EndDate" -> "20101230", "otherParticipantInstanceIdentifier" -> participiant0Cid,
        "otherParticipantUpdateTimestamp" -> participiant0Ts)

      testStubDataFromAPI(list(0), expectedOutputMap)
      list(0).relationshipEndReason.get shouldNot be(null)

      val userRecord = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)
    }

    "check for active relationship" in {
      val controller = makeFakeController()
      val request = FakeRequest()

      val testData = TestData.Lists.oneActive
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid
      val testTs = testData.user.timestamp.toString()

      val participiant0 = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts = participiant0.partner.timestamp.toString()

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 1
      val expectedOutputMap = Map("participant" -> "Recipient", "creationTimestamp" -> "20150531235901", "participant1StartDate" -> "20011230",
        "relationshipEndReason" -> None, "participant1EndDate" -> None, "otherParticipantInstanceIdentifier" -> participiant0Cid,
        "otherParticipantUpdateTimestamp" -> participiant0Ts)

      testStubDataFromAPI(list(0), expectedOutputMap)
      list(0).relationshipEndReason should be(None)

      val userRecord = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)

    }

    "return TRANSFEROR-NOT-FOUND when transferor is deceased" in {
      val controller = makeFakeController()
      val request = FakeRequest()

      val testData = TestData.Lists.deceased
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid
      val testTs = testData.user.timestamp.toString()

      val participiant0 = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts = participiant0.partner.timestamp.toString()

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "status" \ "status_code").get.toString() shouldBe "\"TAMC:ERROR:TRANSFEROR-NOT-FOUND\""
    }

    "return TRANSFEROR-NOT-FOUND when transferor not found" in {
      val controller = makeFakeController()
      val request = FakeRequest()

      val testData = TestData.Lists.tansfrorNotFound
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid
      val testTs = testData.user.timestamp.toString()

      val participiant0 = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts = participiant0.partner.timestamp.toString()

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "status" \ "status_code").get.toString() shouldBe "\"TAMC:ERROR:TRANSFEROR-NOT-FOUND\""
    }

  }

  private def testStubDataFromAPI(result: TestRelationshipRecord, expectedOutputMap: Map[String, Any]) = {
    result.participant shouldBe expectedOutputMap.get("participant").get
    result.creationTimestamp shouldBe expectedOutputMap.get("creationTimestamp").get
    result.participant1StartDate shouldBe expectedOutputMap.get("participant1StartDate").get
    result.relationshipEndReason.getOrElse(None) shouldBe expectedOutputMap.get("relationshipEndReason").get
    result.participant1EndDate.getOrElse(None) shouldBe expectedOutputMap.get("participant1EndDate").get
    result.otherParticipantInstanceIdentifier shouldBe expectedOutputMap.get("otherParticipantInstanceIdentifier").get
    result.otherParticipantUpdateTimestamp shouldBe expectedOutputMap.get("otherParticipantUpdateTimestamp").get
  }

  private def testUserRecord(result: UserRecord, expectedOutputMap: Map[String, Any]) = {
    result.cid shouldBe expectedOutputMap.get("cid").get
    result.timestamp shouldBe expectedOutputMap.get("timestamp").get
  }

  "Calling update relationship for logged in person" should {

    "check if update (cancel) relationship for transferor then response is successfull" in {

      val testInput = TestData.Updates.cancel
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController()
      val testData = s"""{"request":{"participant1":{"instanceIdentifier":"${recipientCid}","updateTimestamp":"${recipientTs}"},"participant2":{"updateTimestamp":"${transferorTs}"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Cancelled by Transferor","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }

    "check if update (reject) relationship for recipient then response is successfull" in {

      val testInput = TestData.Updates.reject
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController()
      val testData = s"""{"request":{"participant1":{"instanceIdentifier":"${recipientCid}","updateTimestamp":"${recipientTs}"},"participant2":{"updateTimestamp":"${transferorTs}"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Rejected by Recipient","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Recipient", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }

    "check if update (divorce) relationship for transferor then response is successfull" in {

      val testInput = TestData.Updates.divorceTr
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController()
      val testData = s"""{"request":{"participant1":{"instanceIdentifier":"${recipientCid}","updateTimestamp":"${recipientTs}"},"participant2":{"updateTimestamp":"${transferorTs}"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Divorce/Separation","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }

    "check if update (divorce) relationship for recipient then response is successfull" in {

      val testInput = TestData.Updates.divorceRec
      val transferorNino = Nino(testInput.transferor.nino)
      val transferorCid = testInput.transferor.cid.cid
      val recipientNino = Nino(testInput.recipient.nino)
      val recipientCid = testInput.recipient.cid.cid
      val transferorTs = testInput.transferor.timestamp.toString()
      val recipientTs = testInput.recipient.timestamp.toString()

      val controller = makeFakeController()
      val testData = s"""{"request":{"participant1":{"instanceIdentifier":"${recipientCid}","updateTimestamp":"${recipientTs}"},"participant2":{"updateTimestamp":"${transferorTs}"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Divorce/Separation","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Recipient", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }
  }
}
