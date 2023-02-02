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

package controllers.auth

import controllers.auth.AuthConnector
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, BodyParsers}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import test_utils.UnitSpec
import uk.gov.hmrc.auth.core._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future


class AuthActionSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val authAction: AuthAction = new AuthActionImpl(mockAuthConnector, inject[BodyParsers.Default])(global)
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()

  class Harness() {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Ok("") }
  }

  "A user with no active session" should {
    "return UNAUTHORIZED" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val controller = new Harness()
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe UNAUTHORIZED
    }
  }

  "A user with insufficient confidence level" should {
    "return UNAUTHORIZED" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientConfidenceLevel()))
      val controller = new Harness()
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe UNAUTHORIZED
    }
  }

  "A user that is logged in" must {
    "be allowed access" in {
      when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
        .thenReturn(Future.successful(()))

      val controller = new Harness()

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
    }
  }
}
