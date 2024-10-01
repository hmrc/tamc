/*
 * Copyright 2024 HM Revenue & Customs
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

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers.auth
//
//import connectors.PertaxConnector
//import models.PertaxResponse
//import models.admin.PertaxBackendToggle
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.when
//import play.api.http.Status.{BAD_GATEWAY, SERVICE_UNAVAILABLE, UNAUTHORIZED}
//import play.api.test.FakeRequest
//import play.api.test.Helpers.stubControllerComponents
//import test_utils.UnitSpec
//import uk.gov.hmrc.http.UpstreamErrorResponse
//import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
//import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
//
//import scala.concurrent.Future
//
//class PertaxAuthActionSpec extends UnitSpec {
//
//  val mockPertaxConnector = mock[PertaxConnector]
//  val mockFeatureFlagService = mock[FeatureFlagService]
//
//  val pertaxAuthAction = new PertaxAuthAction(mockPertaxConnector, mockFeatureFlagService, stubControllerComponents())
//
//
//  "filter" should {
//    "return None" when {
//      "toggle is enabled and pertax code is ACCESS_GRANTED" in {
//        when(mockFeatureFlagService.get(PertaxBackendToggle))
//          .thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))
//        when(mockPertaxConnector.pertaxPostAuthorise(any(), any()))
//          .thenReturn(Future.successful(Right(PertaxResponse("ACCESS_GRANTED", "Any old message"))))
//
//        val request = FakeRequest()
//        val result = await(pertaxAuthAction.filter(request))
//
//        result shouldBe None
//      }
//
//      "toggle is disabled" in {
//        when(mockFeatureFlagService.get(PertaxBackendToggle))
//          .thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = false)))
//
//        val request = FakeRequest()
//        val result = await(pertaxAuthAction.filter(request))
//
//        result shouldBe None
//      }
//    }
//
//    "Return Unauthorised when toggle is enabled and pertax returns Right that is not ACCESS_GRANTED" in {
//      when(mockFeatureFlagService.get(PertaxBackendToggle))
//        .thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))
//      when(mockPertaxConnector.pertaxPostAuthorise(any(), any()))
//        .thenReturn(Future.successful(Right(PertaxResponse("CODE", "Any old message"))))
//      val request = FakeRequest()
//      val result = await(pertaxAuthAction.filter(request))
//
//      result.map(status(_) shouldBe UNAUTHORIZED)
//    }
//
//    "Return BadGateway when toggle is anabled and pertax returns Left" in {
//      when(mockFeatureFlagService.get(PertaxBackendToggle))
//        .thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))
//      when(mockPertaxConnector.pertaxPostAuthorise(any(), any()))
//        .thenReturn(Future.successful(Left(UpstreamErrorResponse("Error message", SERVICE_UNAVAILABLE, BAD_GATEWAY))))
//      val request = FakeRequest()
//      val result = await(pertaxAuthAction.filter(request))
//
//      result.map(status(_) shouldBe BAD_GATEWAY)
//    }
//  }
//}
