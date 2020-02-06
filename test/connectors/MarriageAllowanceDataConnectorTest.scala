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

import com.codahale.metrics.Timer
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.MarriageAllowanceDataConnector.baseUrl
import errors._
import metrics.Metrics
import models._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import test_utils.WireMockHelper
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.test.UnitSpec
import utils.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global

class MarriageAllowanceDataConnectorTest extends UnitSpec with GuiceOneAppPerSuite with WireMockHelper with MockitoSugar {

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.marriage-allowance-des.port" -> server.port()
      )
      .build()
  }

  trait FindRecipientSetup {

    val generatedNino = new Generator().nextNino
    val surname = "testSurname"
    val forename1 = "testForename1"
    val gender = "M"
    val request = FindRecipientRequest(name = forename1, lastName = surname, gender = Gender(gender), nino = generatedNino)
    val queryString = s"surname=${utils.encodeQueryStringValue(surname)}&forename1=${utils.encodeQueryStringValue(forename1)}&gender=${utils.encodeQueryStringValue(gender)}"
    val url = s"/marriage-allowance/citizen/${generatedNino.nino}/check?${queryString}"

    val mockTimerContext = mock[Timer.Context]
    when(connector.metrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)
  }

  lazy val connector = new MarriageAllowanceDataConnector {
    override val httpGet = WSHttp
    override val httpPost = WSHttp
    override val httpPut = WSHttp
    override val serviceUrl = baseUrl("marriage-allowance-des")
    override val urlHeaderEnvironment = "test"
    override val urlHeaderAuthorization = "Bearer"
    override val metrics = mock[Metrics]
  }

  val instanceIdentifier: Cid = 123456789
  val updateTimestamp: Timestamp = "20200116155359011123"


  def expectedJson(reasonCode: Int, returnCode: Int): JsValue = Json.parse(
    s"""{
          "Jfwk1012FindCheckPerNoninocallResponse": {
            "Jfwk1012FindCheckPerNoninoExport": {
              "@exitStateType": "0",
              "@exitState": "0",
              "OutItpr1Person": {
                "InstanceIdentifier": $instanceIdentifier,
                "UpdateTimestamp": "$updateTimestamp"
              },
              "OutWCbdParameters": {
                "SeverityCode": "W",
                "DataStoreStatus": "S",
                "OriginServid": 9999,
                "ContextString": "ITPR1311_PER_DETAILS_FIND_S",
                "ReturnCode": $reasonCode,
                "ReasonCode": $returnCode,
                "Checksum": "a234jnjbhr9ui83"
              }
            }
          }
      }""")


  "findRecipient" should {

    "return a UserRecord given a valid request" when {

      "the return code and response code are both 1" in { new FindRecipientSetup {

          val reasonCode = 1
          val returnCode = 1

          val json = expectedJson(reasonCode, returnCode)

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(ok(json.toString()))
          )
          val expectedResult = UserRecord(instanceIdentifier, updateTimestamp)
          val result = await(connector.findRecipient(request))

          result shouldBe Right(expectedResult)
        }
      }

      "a valid nino contains spaces" in { new FindRecipientSetup {

            val reasonCode = 1
            val returnCode = 1

            val json = expectedJson(reasonCode, returnCode)

            server.stubFor(
              get(urlEqualTo(url))
                .willReturn(ok(json.toString()))
            )

            val expectedResult = UserRecord(instanceIdentifier, updateTimestamp)
            val ninoWithSpaces = Nino(generatedNino.formatted)

            val result = await(connector.findRecipient(request))

            result shouldBe Right(expectedResult)
        }
      }
    }

    "return a ResponseValidator error given non valid Json" in { new FindRecipientSetup {

      val nonValidJson = s"""{
          "Jfwk1012FindCheckPerNoninocallResponse": {
            "Jfwk1012FindCheckPerNoninoExport": {
              "@exitStateType": "0",
              "@exitState": "0",
              "OutItpr1Person": {
                "InstanceIdentifier": $instanceIdentifier
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
      }"""

      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(ok(nonValidJson.toString()))
      )

      val result = await(connector.findRecipient(request))

      result shouldBe Left(ResponseValidationError)

      }
    }

    "return a CodedErrorResponse" when {

      "a response states a nino must be supplied" in { new FindRecipientSetup {

        val returnCode = -1011
        val reasonCode = 2039

        val json = expectedJson(reasonCode, returnCode)

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val result = await(connector.findRecipient(request))

        result shouldBe Left(FindRecipientCodedErrorResponse(returnCode, reasonCode, "Nino must be supplied"))
      }
      }

      "a response states only one of Nino or temporary reference must be supplied" in { new FindRecipientSetup {

        val returnCode = -1011
        val reasonCode = 2040

        val json = expectedJson(reasonCode, returnCode)

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val result = await(connector.findRecipient(request))

        result shouldBe Left(FindRecipientCodedErrorResponse(returnCode, reasonCode, "Only one of Nino or Temporary Reference must be supplied"))
      }
      }

      "a response states the confidence check surname has not been supplied" in { new FindRecipientSetup {

        val returnCode = -1011
        val reasonCode = 2061

        val json = expectedJson(reasonCode, returnCode)

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val result = await(connector.findRecipient(request))

        result shouldBe Left(FindRecipientCodedErrorResponse(returnCode, reasonCode, "Confidence Check Surname not supplied"))
      }
      }

      "the returnCode and reasonCode state the nino is not found" in new FindRecipientSetup {

        val reasonCode = 2016
        val returnCode = -1011

        val json = expectedJson(reasonCode, returnCode)

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val result = await(connector.findRecipient(request))
        result shouldBe Left(FindRecipientCodedErrorResponse(returnCode, reasonCode, "Nino not found and Nino not found in merge trail"))

      }

      "the returnCode and reasonCode state multiple ninos found" in new FindRecipientSetup {

        val reasonCode = 2017
        val returnCode = -1011

        val json = expectedJson(reasonCode, returnCode)

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val result = await(connector.findRecipient(request))

        result shouldBe Left(FindRecipientCodedErrorResponse(returnCode, reasonCode, "Nino not found and Nino found in multiple merge trails"))

      }


    }

    "return an UnhandledStatusError type" when {

      "an unsupported status code is received" in {
        new FindRecipientSetup {
          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse().withStatus(413).withBody("Payload too large"))
          )

          val result = await(connector.findRecipient(request))

          result shouldBe Left(UnhandledStatusError)

        }
      }

      "uncatered return code and reason codes are received" in { new FindRecipientSetup {

          val reasonCode = 2
          val returnCode = 2

          val json = expectedJson(reasonCode, returnCode)

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(ok(json.toString()))
          )

          val result = await(connector.findRecipient(request))

          result shouldBe Left(UnhandledStatusError)

        }
      }
    }
  }
}
