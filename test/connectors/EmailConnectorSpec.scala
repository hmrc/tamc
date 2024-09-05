/*
 * Copyright 2023 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, post, urlEqualTo}
import config.ApplicationConfig
import metrics.TamcMetrics
import models._
import org.mockito.Mockito.{when, reset => resetMock}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import test_utils.UnitSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

import scala.concurrent.ExecutionContext.global

class EmailConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with HttpClientV2Support with WireMockSupport {


  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockMetrics: TamcMetrics = mock[TamcMetrics]
  val mockTimerContext: Timer.Context = mock[Timer.Context]
  when(mockMetrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMock(mockMetrics)
    resetMock(mockTimerContext)
    when(mockAppConfig.EMAIL_URL).thenReturn(wireMockUrl)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(Map("microservice.services.email.port" -> wireMockPort))
    .overrides(
      bind[HttpClientV2].toInstance(mock[HttpClientV2])
    ).build()

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val connector: EmailConnector = new EmailConnector(httpClientV2, mockAppConfig)(global)

  val addressList: List[EmailAddress] = List(new EmailAddress("bob@test.com"))
  val params: Map[String, String] = Map("a" -> "b")
  val request: SendEmailRequest = SendEmailRequest(addressList, "tamc_recipient_rejects_retro_yr", params, force = false)


  "sendEmail" should {
    "return Unit" when {
      "an email is sent " in {
        wireMockServer.stubFor(
          post(urlEqualTo("/hmrc/email"))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        val response = await(connector.sendEmail(request))
        response.toOption.get shouldBe a[Unit]
      }
    }
  }
}