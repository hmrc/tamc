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

package test_utils

import models.{Cid, FindRecipientRequest, Gender, Timestamp}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.libs.json.{JsNumber, JsString}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.BigDecimal.long2bigDecimal

class TestUtilityTest extends UnitSpec with TestUtility with OneAppPerSuite {

  override implicit lazy val app: Application = fakeApplication

  "TestUtilityTest" should {

    "sanity check for findCitizen" in {

      val user = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP5A)
      val userNino = user.nino
      val userCid = user.cid.cid
      val userTs = user.timestamp

      val controller = makeFakeController()
      val request = FakeRequest()
      implicit val hc = HeaderCarrier()
      val result = controller.marriageAllowanceService.dataConnector.findCitizen(Nino(userNino))
      ScalaFutures.whenReady(result)(json => {
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "InstanceIdentifier").get shouldBe JsNumber(userCid)
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "UpdateTimestamp").get shouldBe JsString(userTs)
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "FirstForename").get shouldBe JsString("Firstnamefivefivefivefivefivefivefivefivefive")
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "Surname").get shouldBe JsString("Lastnamefivefivefivefivefivefivefivefivefive")
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "InstanceIdentifier").as[Cid] shouldBe userCid
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "UpdateTimestamp").as[Timestamp] shouldBe userTs
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "FirstForename").as[String] shouldBe "Firstnamefivefivefivefivefivefivefivefivefive"
        (json \ "Jtpr1311PerDetailsFindcallResponse" \ "Jtpr1311PerDetailsFindExport" \ "OutItpr1Person" \ "Surname").as[String] shouldBe "Lastnamefivefivefivefivefivefivefivefivefive"
      })
    }

    "sanity check for findRecipient" in {
      val controller = makeFakeController()
      val request = FakeRequest()
      implicit val hc = HeaderCarrier()

      val recipient = TestData.Recipients.recHasNoAllowance
      val recipientNino = recipient.citizen.nino
      val recipientCid = recipient.citizen.cid.cid

      val recipientData = FindRecipientRequest("fgh", "asd", Gender("F"), Nino(recipientNino))
      val result = controller.marriageAllowanceService.dataConnector.findRecipient(recipientData)
      ScalaFutures.whenReady(result)(json =>
        (json \ "Jfwk1012FindCheckPerNoninocallResponse" \ "Jfwk1012FindCheckPerNoninoExport" \ "OutItpr1Person" \ "InstanceIdentifier").get shouldBe JsNumber(recipientCid))
    }

    "sanity check for listRelationship" in {
      val testData = TestData.Lists.oneActiveOneHistoric
      val testNino = Nino(testData.user.nino)
      val testCid = testData.user.cid.cid
      val participiant0 = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts = participiant0.partner.timestamp.toString()

      val participiant1 = testData.counterparties(1)
      val participiant1Cid: String = participiant1.partner.cid.cid.toString
      val participiant1Ts = participiant1.partner.timestamp.toString()

      val controller = makeFakeController()
      val request = FakeRequest()
      implicit val hc = HeaderCarrier()
      val result = controller.marriageAllowanceService.dataConnector.listRelationship(testCid)
      ScalaFutures.whenReady(result)(json => {
        ((json \ "relationships") (0) \ "otherParticipantUpdateTimestamp").get shouldBe JsString(participiant0Ts)
        ((json \ "relationships") (1) \ "otherParticipantUpdateTimestamp").get shouldBe JsString(participiant1Ts)
      })
    }
  }
}
