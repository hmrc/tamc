/*
 * Copyright 2022 HM Revenue & Customs
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
import errors._
import metrics.TamcMetrics
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when, reset => resetMock}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.libs.json.{JsValue, Json}
import test_utils.{UnitSpec, WireMockHelper}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class MarriageAllowanceDESConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with WireMockHelper with BeforeAndAfterEach {

  val mockMetrics: TamcMetrics = mock[TamcMetrics]
  val mockHttp: HttpClient = mock[HttpClient]
  val mockTimerContext: Timer.Context = mock[Timer.Context]
  when(mockMetrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMock(mockMetrics, mockHttp, mockTimerContext)
  }

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.marriage-allowance-des.host" -> "127.0.0.1",
        "microservice.services.marriage-allowance-des.port" -> server.port(),
        "microservice.services.marriage-allowance-des.environment" -> "test",
        "microservice.services.marriage-allowance-des.authorization-token" -> "Bearer"
      )
      .build()
  }

  lazy val connector: MarriageAllowanceDESConnector = app.injector.instanceOf[MarriageAllowanceDESConnector]

  val generatedNino: Nino = new Generator().nextNino
  val url = s"/marriage-allowance/citizen/${generatedNino.nino}/check"

  def findRecipientRequest(nino: Nino = generatedNino): FindRecipientRequest = {
    FindRecipientRequest(name = "testForename1", lastName = "testLastName", gender = Gender("M"), nino)
  }

  val instanceIdentifier: Cid = 123456789
  val updateTimestamp: Timestamp = "20200116155359011123"
  val requestId: RequestId = RequestId(Random.alphanumeric.take(10).mkString)
  val sessionId: SessionId = SessionId(Random.alphanumeric.take(10).mkString)
  implicit val hc: HeaderCarrier = HeaderCarrier().copy(requestId = Some(requestId), sessionId = Some(sessionId))
  lazy val processingCodeOK: Int = connector.ProcessingOK
  val uuidRegex = """[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}"""

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
                "ReturnCode": $returnCode,
                "ReasonCode": $reasonCode,
                "Checksum": "a234jnjbhr9ui83"
              }
            }
          }
      }""")

  "findRecipient" should {
    "return a UserRecord given valid Json" when {
      "the return code and response code are both 1" in {
        val json = expectedJson(processingCodeOK, processingCodeOK)
        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val expectedResult = UserRecord(instanceIdentifier, updateTimestamp)
        val result = await(connector.findRecipient(findRecipientRequest()))

        result shouldBe Right(expectedResult)

        server.verify(
          postRequestedFor(urlEqualTo(url))
            .withHeader(HeaderNames.authorisation, equalTo(connector.urlHeaderAuthorization))
            .withHeader(HeaderNames.xRequestId, equalTo(requestId.value))
            .withHeader(HeaderNames.xSessionId, equalTo(sessionId.value))
            .withHeader("Environment", equalTo(connector.urlHeaderEnvironment))
            .withHeader("CorrelationId", matching(uuidRegex)))
      }

      "a valid nino is provided, which contains spaces" in {
        val json = expectedJson(processingCodeOK, processingCodeOK)

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )

        val expectedResult = UserRecord(instanceIdentifier, updateTimestamp)
        val ninoWithSpaces = Nino(generatedNino.formatted)

        val result = await(connector.findRecipient(findRecipientRequest(ninoWithSpaces)))

        result shouldBe Right(expectedResult)
      }
    }

    "contain the correct headers to send to DES" in {
      val json = expectedJson(processingCodeOK, processingCodeOK)
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(ok(json.toString()))
      )
      await(connector.findRecipient(findRecipientRequest()))

      server.verify(postRequestedFor(urlEqualTo(url))
        .withHeader(HeaderNames.authorisation, equalTo(connector.urlHeaderAuthorization))
        .withHeader(HeaderNames.xRequestId, equalTo(requestId.value))
        .withHeader(HeaderNames.xSessionId, equalTo(sessionId.value))
        .withHeader("Environment", equalTo(connector.urlHeaderEnvironment))
        .withHeader("CorrelationId", matching(uuidRegex))
      )
    }

    "return a ResponseValidator error given non valid Json" in {
      val nonValidJson =
        s"""{
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
          .willReturn(ok(nonValidJson))
      )

      val result = await(connector.findRecipient(findRecipientRequest()))

      result shouldBe Left(ResponseValidationError)

    }

    "return a specific error type " when {
      "an error response is received" in {
        val reasonCodes =
          Table(
            ("HTTP status code", "Error Type"),
            (TOO_MANY_REQUESTS, TooManyRequestsError),
            (BAD_REQUEST, BadRequestError),
            (INTERNAL_SERVER_ERROR, ServerError),
            (SERVICE_UNAVAILABLE, ServiceUnavailableError),
            (499, TimeOutError),
            (GATEWAY_TIMEOUT, TimeOutError),
            (413, UnhandledStatusError),
            (BAD_GATEWAY, BadGatewayError)
          )

        forAll(reasonCodes) { (statusCode: Int, errorType: StatusError) =>
          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse().withStatus(statusCode))
          )

          val result = await(connector.findRecipient(findRecipientRequest()))

          result shouldBe Left(errorType)

        }
      }
    }

    //TODO These are mocking HttpClient. We shouldn't be mocking this however functioanlity exists to capture these errors.
    "return a TimeOutError " when {
      "a GatewayTimeout is received" in {
        val injector: Injector = GuiceApplicationBuilder()
          .overrides(
            bind[TamcMetrics].toInstance(mockMetrics),
            bind[HttpClient].toInstance(mockHttp)
          ).injector()

        val connector: MarriageAllowanceDESConnector = injector.instanceOf[MarriageAllowanceDESConnector]

        when(mockMetrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)

        when(mockHttp.POST(ArgumentMatchers.contains(url), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new GatewayTimeoutException("Gateway Timeout")))

        val result = await(connector.findRecipient(findRecipientRequest()))

        result shouldBe Left(TimeOutError)

      }
    }

    "return a BadGateway error" when {
      "a BadGatewayException is received" in {
        val injector: Injector = GuiceApplicationBuilder()
          .overrides(
            bind[TamcMetrics].toInstance(mockMetrics),
            bind[HttpClient].toInstance(mockHttp)
          ).injector()

        val connector: MarriageAllowanceDESConnector = injector.instanceOf[MarriageAllowanceDESConnector]

        when(mockMetrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)

        when(mockHttp.POST(ArgumentMatchers.contains(url), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new BadGatewayException("Bad gateway")))


        val result = await(connector.findRecipient(findRecipientRequest()))

        result shouldBe Left(BadGatewayError)
      }
    }

    "return an UnhandledStatusError type" when {
      "uncatered return code and reason codes are received" in {
        val reasonCode = 2
        val returnCode = 2

        val json = expectedJson(reasonCode, returnCode)

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(ok(json.toString()))
        )
        val result = await(connector.findRecipient(findRecipientRequest()))

        result shouldBe Left(UnhandledStatusError)
      }
    }

    "return a non fatal error after stopping the timer" in {
      val nonFatalErrorMessage = "an error has occurred"
      when(mockHttp.POST(ArgumentMatchers.contains(url), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException(nonFatalErrorMessage)))
      when(mockMetrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)
      val injector: Injector = GuiceApplicationBuilder()
        .overrides(
          bind[TamcMetrics].toInstance(mockMetrics),
          bind[HttpClient].toInstance(mockHttp)
        ).injector()

      val connector: MarriageAllowanceDESConnector = injector.instanceOf[MarriageAllowanceDESConnector]

      val exception = intercept[RuntimeException] {
        await(connector.findRecipient(findRecipientRequest()))
      }

      exception.getMessage shouldBe nonFatalErrorMessage

      verify(mockTimerContext, times(1)).stop()
    }

    "return a CodedErrorResponse" when {
      "a specific return code and reason code are received" in {
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

        forAll(reasonCodes) { (reasonCode: Int, errorMessage: String) =>

          val json = expectedJson(reasonCode, connector.ErrorReturnCode)

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(ok(json.toString()))
          )

          val result = await(connector.findRecipient(findRecipientRequest()))
          result shouldBe Left(FindRecipientCodedErrorResponse(connector.ErrorReturnCode, reasonCode, errorMessage))
        }
      }
    }
  }

  "findCitizen" should {
    val url = s"/marriage-allowance/citizen/$generatedNino"
    "pass correct headers to des" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(ok("{}"))
      )

      await(connector.findCitizen(generatedNino))

      server.verify(
        getRequestedFor(urlEqualTo(url))
          .withHeader(HeaderNames.authorisation, equalTo(connector.urlHeaderAuthorization))
          .withHeader(HeaderNames.xRequestId, equalTo(requestId.value))
          .withHeader(HeaderNames.xSessionId, equalTo(sessionId.value))
          .withHeader("Environment", equalTo(connector.urlHeaderEnvironment))
          .withHeader("CorrelationId", matching(uuidRegex))
      )
    }
  }

  "listRelationship" should {
    val cid = Random.nextLong()
    val url = s"/marriage-allowance/citizen/$cid/relationships?includeHistoric=true"

    "pass correct headers to des" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(ok("{}"))
      )

      await(connector.listRelationship(cid))

      server.verify(
        getRequestedFor(urlEqualTo(url))
          .withHeader(HeaderNames.authorisation, equalTo(connector.urlHeaderAuthorization))
          .withHeader(HeaderNames.xRequestId, equalTo(requestId.value))
          .withHeader(HeaderNames.xSessionId, equalTo(sessionId.value))
          .withHeader("Environment", equalTo(connector.urlHeaderEnvironment))
          .withHeader("CorrelationId", matching(uuidRegex))
      )
    }
  }

  "sendMultiYearCreateRelationshipRequest" should {
    val relType = Random.alphanumeric.take(5).mkString
    val recipientCid = Random.alphanumeric.take(5).mkString
    val relationshipRequest =
      MultiYearDesCreateRelationshipRequest(recipientCid, "", "", "", None, None)
    val url = s"/marriage-allowance/02.00.00/citizen/$recipientCid/relationship/$relType"

    "pass correct headers to des" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(ok("{}"))
      )

      await(connector.sendMultiYearCreateRelationshipRequest(relType, relationshipRequest))

      server.verify(
        postRequestedFor(urlEqualTo(url))
          .withHeader(HeaderNames.authorisation, equalTo(connector.urlHeaderAuthorization))
          .withHeader(HeaderNames.xRequestId, equalTo(requestId.value))
          .withHeader(HeaderNames.xSessionId, equalTo(sessionId.value))
          .withHeader("Environment", equalTo(connector.urlHeaderEnvironment))
          .withHeader("CorrelationId", matching(uuidRegex))
      )
    }
  }

  "updateAllowanceRelationship" should {
    val instanceId = Random.alphanumeric.take(5).mkString
    val url = s"/marriage-allowance/citizen/$instanceId/relationship"
    val relationshipRequest = DesUpdateRelationshipRequest(
      DesRecipientInformation(instanceId, ""),
      DesTransferorInformation(""),
      DesRelationshipInformation("", "", "")
    )

    "pass correct headers to des" in {
      server.stubFor(
        put(urlEqualTo(url))
          .willReturn(ok("{}"))
      )

      await(connector.updateAllowanceRelationship(relationshipRequest))

      server.verify(
        putRequestedFor(urlEqualTo(url))
          .withHeader(HeaderNames.authorisation, equalTo(connector.urlHeaderAuthorization))
          .withHeader(HeaderNames.xRequestId, equalTo(requestId.value))
          .withHeader(HeaderNames.xSessionId, equalTo(sessionId.value))
          .withHeader("Environment", equalTo(connector.urlHeaderEnvironment))
          .withHeader("CorrelationId", matching(uuidRegex))
      )
    }
  }
}
