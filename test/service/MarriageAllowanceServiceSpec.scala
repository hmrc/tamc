/*
 * Copyright 2019 HM Revenue & Customs
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

package service

import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import fixtures._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import services.MarriageAllowanceService
import uk.gov.hmrc.http.{HeaderCarrier, _}
import _root_.controllers.FakeTamcApplication
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class MarriageAllowanceServiceSpec extends UnitSpec with GuiceOneAppPerSuite with FakeTamcApplication {

  implicit lazy override val app: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  )
  .overrides(bind(classOf[MarriageAllowanceDataConnector]).to[MockDeceasedDataConnector])
  .overrides(bind(classOf[EmailConnector]).to[MockEmailConnector])
  .build()

  "when request is sent with deceased recipient in MarriageAllowanceService" should {
    "return a BadRequestException" in {
        val service = app.injector.instanceOf[MarriageAllowanceService]
        val multiYearCreateRelationshipRequest = MultiYearCreateRelationshipRequestHolderFixture.multiYearCreateRelationshipRequestHolder
        val response = service.createMultiYearRelationship(multiYearCreateRelationshipRequest, "GDS")(HeaderCarrier(), implicitly)
        intercept[BadRequestException] {
          await(response)
        }
    }
  }
}
