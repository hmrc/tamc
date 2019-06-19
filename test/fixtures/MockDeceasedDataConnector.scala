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

package fixtures

import connectors.MarriageAllowanceDataConnector
import javax.inject.Inject
import models.MultiYearDesCreateRelationshipRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{BadRequestException, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class MockDeceasedDataConnector @Inject()(http: HttpClient,
                                          environment: Environment,
                                          configuration: Configuration,
                                           runMode: RunMode) extends MarriageAllowanceDataConnector(http, environment, configuration, runMode) {
  override val serviceUrl = ""
  override val urlHeaderEnvironment = ""
  override val urlHeaderAuthorization = "foo"

  override def sendMultiYearCreateRelationshipRequest(relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    Future.failed(new BadRequestException("{\"reason\": \"Participant is deceased\"}"))
  }
}