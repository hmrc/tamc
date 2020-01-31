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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import connectors.MarriageAllowanceDESConnector.baseUrl
import models.FindRecipientRequestDes
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import test_utils.WireMockHelper
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.test.UnitSpec
import utils.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global

class MarriageAllowanceDESConnectorTest extends UnitSpec with GuiceOneAppPerSuite with WireMockHelper with MockitoSugar {

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.marriage-allowance-des.port" -> server.port()
      )
      .build()
  }

  lazy val connector = new MarriageAllowanceDESConnector {
    override val httpGet = WSHttp
    override val httpPost = WSHttp
    override val httpPut = WSHttp
    override val serviceUrl = baseUrl("marriage-allowance-des")
    override val urlHeaderEnvironment = "test"
    override val urlHeaderAuthorization = "Bearer"
  }


  "findRecipient" should {

    "return JsValue" in {

      val generatedNino = new Generator().nextNino.nino

      val request = FindRecipientRequestDes("testSurname", "testForename1", Some("testForename2"), Some("M"))

      val expectedJson = Json.parse("""{
          "Jfwk1012FindCheckPerNoninocallResponse": {
            "Jfwk1012FindCheckPerNoninoExport": {
              "@exitStateType": "0",
              "@exitState": "0",
              "OutItpr1Person": {
                "InstanceIdentifier": 123456789,
                "UpdateTimestamp": "20200116155359011123"
              },
              "OutWCbdParameters": {
                "SeverityCode": "W",
                "DataStoreStatus": "S",
                "OriginServid": 9999,
                "ContextString": "ITPR1311_PER_DETAILS_FIND_S",
                "ReturnCode": 1,
                "ReasonCode": 1,
                "Checksum": "a234jnjbhr9ui83"
              }
            }
          }
      }""")

      val url = s"/marriage-allowance/citizen/$generatedNino/check"

      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(ok(expectedJson.toString()))
      )

      val result = await(connector.findRecipient(generatedNino, request))

      result shouldBe Right(expectedJson)
     }
  }
}
