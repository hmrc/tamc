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
import connectors.MarriageAllowanceDESConnector.baseUrl
import errors._
import metrics.Metrics
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, verify, times}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import test_utils.WireMockHelper
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.test.UnitSpec
import utils.WSHttp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MarriageAllowanceDESConnectorTest extends UnitSpec with GuiceOneAppPerSuite with WireMockHelper with MockitoSugar {

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.marriage-allowance-des.port" -> server.port()
      )
      .build()
  }

  trait FindRecipientSetup {

    val generatedNino = new Generator().nextNino
    val request = FindRecipientRequest(name = "testForename1", lastName = "testLastName", gender = Gender("M"), nino = generatedNino)
    val url = s"/marriage-allowance/citizen/${generatedNino.nino}/check"

    val mockTimerContext = mock[Timer.Context]
    when(connector.metrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)

  }

  lazy val connector = new MarriageAllowanceDESConnector {
    override val httpGet = WSHttp
    override val httpPost = WSHttp
    override val httpPut = WSHttp
    override val serviceUrl = baseUrl("marriage-allowance-des")
    override val urlHeaderEnvironment = "test"
    override val urlHeaderAuthorization = "Bearer"
    override val metrics = mock[Metrics]
  }

  lazy val mockedPostConnector = new MarriageAllowanceDESConnector {
    override val httpGet: HttpGet = WSHttp
    override val httpPost: HttpPost = mock[WSHttp]
    override val httpPut = WSHttp
    override val serviceUrl = baseUrl("marriage-allowance-des")
    override val urlHeaderEnvironment = "test"
    override val urlHeaderAuthorization = "Bearer"
    override val metrics = mock[Metrics]
  }

  val instanceIdentifier: Cid = 123456789
  val updateTimestamp: Timestamp = "20200116155359011123"
  implicit val hc = HeaderCarrier()
  lazy val processingCodeOK = connector.ProcessingOK


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

    "return a UserRecord given valid Json" when {

      "the return code and response code are both 1" in new FindRecipientSetup {

        val json = expectedJson(processingCodeOK, processingCodeOK)

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )
        val expectedResult = UserRecord(instanceIdentifier, updateTimestamp)
        val result = await(connector.findRecipient(request))

        result shouldBe Right(expectedResult)
      }


      "a valid nino contains spaces" in new FindRecipientSetup {

        val json = expectedJson(processingCodeOK, processingCodeOK)

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val expectedResult = UserRecord(instanceIdentifier, updateTimestamp)
        val ninoWithSpaces = Nino(generatedNino.formatted)

        val result = await(connector.findRecipient(request))

        result shouldBe Right(expectedResult)
      }
    }

    "contain the correct headers to send to DES" in new FindRecipientSetup {

      val uuidRegex = """[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}"""
      val json = expectedJson(processingCodeOK, processingCodeOK)


      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(ok(json.toString()))
      )

      val result = await(connector.findRecipient(request))

      server.verify(postRequestedFor(urlEqualTo(url))
        .withHeader("Authorization", equalTo(connector.urlHeaderAuthorization))
        .withHeader("Environment", equalTo(connector.urlHeaderEnvironment))
        .withHeader("CorrelationId", matching(uuidRegex))
      )
    }


    "return a ResponseValidator error given non valid Json" in  new FindRecipientSetup {

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
        post(urlEqualTo(url))
          .willReturn(ok(nonValidJson.toString()))
      )

      val result = await(connector.findRecipient(request))

      result shouldBe Left(ResponseValidationError)

    }

    "return a specific error type " when {

      "an error response is received" in new FindRecipientSetup {

        val reasonCodes =

          Table(
            ("HTTP status code", "Error Type"),
            (TOO_MANY_REQUESTS, TooManyRequestsError),
            (BAD_REQUEST, BadRequestError),
            (INTERNAL_SERVER_ERROR, ServerError),
            (SERVICE_UNAVAILABLE, ServiceUnavailableError),
            (499, TimeOutError),
            (GATEWAY_TIMEOUT, TimeOutError),
            (413, UnhandledStatusError)
          )

        forAll (reasonCodes) { (statusCode: Int, errorType: StatusError) =>
          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse().withStatus(statusCode))
          )

          val result = await(connector.findRecipient(request))

          result shouldBe Left(errorType)

        }
      }
    }

    "return a TimeOutError " when {

      "a GatewayTimeout is received" in new FindRecipientSetup {

        when(mockedPostConnector.metrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)
        when(mockedPostConnector.httpPost.POST(ArgumentMatchers.contains(url), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new GatewayTimeoutException("timeout")))

        val result = await(mockedPostConnector.findRecipient(request))

        result shouldBe Left(TimeOutError)

      }
    }

    "return a BadGateway error" when {

      "a BadGatewayException is received" in new FindRecipientSetup {

        when(mockedPostConnector.httpPost.POST(ArgumentMatchers.contains(url), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new BadGatewayException("Bad gateway")))

        val result = await(mockedPostConnector.findRecipient(request))

        result shouldBe Left(BadGatewayError)
      }
    }

    "return an UnhandledStatusError type" when {

      "uncatered return code and reason codes are received" in new FindRecipientSetup {

        val reasonCode = 2
        val returnCode = 2

        val json = expectedJson(reasonCode, returnCode)

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val result = await(connector.findRecipient(request))

        result shouldBe Left(UnhandledStatusError)

      }
    }

    "return a non fatal error after stopping the timer" in new FindRecipientSetup {

      val nonFatalErrorMessage = "an error has occurred"

      when(mockedPostConnector.httpPost.POST(ArgumentMatchers.contains(url), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException(nonFatalErrorMessage)))

      when(mockedPostConnector.metrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)

      val exception = intercept[RuntimeException]{
        await(mockedPostConnector.findRecipient(request))
      }

      exception.getMessage shouldBe nonFatalErrorMessage

      verify(mockTimerContext, times(1)).stop()
    }

    "return a CodedErrorResponse" when {

      "a specific return code and reason code are received" in new FindRecipientSetup {

        val reasonCodes =

          Table(
            ("DES reason code", "DES error message"),
            (connector.NinoRequired, "Nino must be supplied"),
            (connector.OnlyOneNinoOrTempReference, "Only one of Nino or Temporary Reference must be supplied"),
            (connector.SurnameNotSupplied, "Confidence Check Surname not supplied"),
            (connector.ConfidenceCheck, "Confidence check failed"),
            (connector.NinoNotFound, "Nino not found and Nino not found in merge trail"),
            (connector.MultipleNinosInMergeTrail, "Nino not found and Nino found in multiple merge trails")
          )

        forAll (reasonCodes) { (reasonCode: Int, errorMessage: String) =>

          val json = expectedJson(reasonCode, connector.ErrorReturnCode)

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(ok(json.toString()))
          )

          val result = await(connector.findRecipient(request))
          result shouldBe Left(FindRecipientCodedErrorResponse(connector.ErrorReturnCode, reasonCode, errorMessage))
        }
      }
    }

  }
}
