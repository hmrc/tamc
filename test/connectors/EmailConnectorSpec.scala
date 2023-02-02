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
import metrics.TamcMetrics
import models._
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{when, reset => resetMock}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import test_utils.UnitSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http._

import scala.concurrent.Future
import scala.reflect.ClassTag

class EmailConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  val mockMetrics: TamcMetrics = mock[TamcMetrics]
  val mockTimerContext: Timer.Context = mock[Timer.Context]
  when(mockMetrics.startTimer(ApiType.FindRecipient)).thenReturn(mockTimerContext)

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMock(mockMetrics)
    resetMock(injected[HttpClient])
    resetMock(mockTimerContext)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClient].toInstance(mock[HttpClient])
    ).build()

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "sendEmail" should {
    "return Unit" when {
      "an email is sent " in {
        val connector: EmailConnector = app.injector.instanceOf[EmailConnector]

        val httpResponse: Future[Either[UpstreamErrorResponse, HttpResponse]] = Future.successful(Right(HttpResponse(200, "")))
        when(injected[HttpClient].POST[SendEmailRequest, Either[UpstreamErrorResponse, HttpResponse]](anyString(), any(), any())(any(), any(), any(), any()))
          .thenReturn(httpResponse)

        val addressList: List[EmailAddress] = List(new EmailAddress("bob@test.com"))
        val params = Map( "a"-> "b")
        val request = SendEmailRequest(addressList, "tamc_recipient_rejects_retro_yr", params, force = false)
        val response = await(connector.sendEmail(request))
        response.toOption.get shouldBe a[Unit]
      }
    }
  }
}