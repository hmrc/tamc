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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import errors.ErrorResponseStatus
import errors.ErrorResponseStatus.{CITIZEN_NOT_FOUND, RECIPIENT_NOT_FOUND, SERVER_ERROR}
import models._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, JsNull}
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeRequest, FakeHeaders}
import play.api.test.Helpers.{status => getStatus, _}
import test_utils.FileHelper._
import test_utils.IntegrationSpec
import uk.gov.hmrc.domain.Generator

class MarriageAllowanceControllerISpec extends IntegrationSpec {

  override def fakeApplication() = GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.marriage-allowance-des.host" -> "127.0.0.1",
    "microservice.services.marriage-allowance-des.port" -> server.port()
  ).build()

  override def beforeEach() = {
    super.beforeEach()
    server.stubFor(post(urlEqualTo("/auth/authorise")).willReturn(ok("{}")))
  }

  val generatedNino = new Generator().nextNino

  "getRecipientRelationship" should {

    List(notFound() -> "404", serverError() -> "500", serviceUnavailable() -> "503", badRequest() -> "400").foreach {
      case (errorResponse, errorCode) =>
        s"return other error for $errorCode" in {

          server.stubFor(post(urlEqualTo(s"/marriage-allowance/citizen/$generatedNino/check")).willReturn(errorResponse))

          val findRecipientRequest = FindRecipientRequest("", "", Gender("M"), generatedNino)
          val json = AnyContentAsJson(Json.toJson(findRecipientRequest))

          val request = FakeRequest(
            method = POST,
            uri = s"/paye/$generatedNino/get-recipient-relationship",
            headers = FakeHeaders(Seq(
              "Authorization" -> "Bearer bearer-token"
            )),
            body = json
          )

          val result = route(fakeApplication(), request)
          val expected = Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND)))

          result.map(getStatus) shouldBe Some(OK)
          result.map(contentAsJson) shouldBe Some(expected)
        }
    }
  }

  "listRelationship" should {

    "return service error code" when {

      val request = FakeRequest(
        method = GET,
        uri = s"/paye/$generatedNino/list-relationship",
        headers = FakeHeaders(Seq(
          "Authorization" -> "Bearer bearer-token"
        )),
        body = JsNull
      )

      List(
        notFound() -> CITIZEN_NOT_FOUND -> "404",
        badRequest() -> ErrorResponseStatus.BAD_REQUEST -> "400",
        serverError() -> SERVER_ERROR -> "500",
        serviceUnavailable() -> ErrorResponseStatus.SERVICE_UNAVILABLE -> "503"
      ).foreach { case ((errorResponse, errorCode), httpErrorCode) =>

        s"citizen endpoint returns $httpErrorCode" in {

          server.stubFor(
            get(urlEqualTo(s"/marriage-allowance/citizen/$generatedNino"))
              .willReturn(errorResponse)
          )

          val result = route(fakeApplication(), request)
          val expected = Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = errorCode)))

          result.map(getStatus) shouldBe Some(OK)
          result.map(contentAsJson) shouldBe Some(expected)
        }

        s"listRelationship endpoint returns $httpErrorCode" in {

          server.stubFor(
            get(urlEqualTo(s"/marriage-allowance/citizen/$generatedNino"))
              .willReturn(ok(loadFile("./it/resources/citizenRecord.json")))
          )

          server.stubFor(
            get(urlEqualTo(s"/marriage-allowance/citizen/123456/relationships?includeHistoric=true"))
              .willReturn(errorResponse)
          )

          val result = route(fakeApplication(), request)
          val expected = Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = errorCode)))

          result.map(getStatus) shouldBe Some(OK)
          result.map(contentAsJson) shouldBe Some(expected)
        }
      }
    }
  }
}
