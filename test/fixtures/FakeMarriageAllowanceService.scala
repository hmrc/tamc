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

import config.ApplicationConfig
import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import javax.inject.Inject
import metrics.Metrics
import models.MultiYearCreateRelationshipRequestHolder
import org.joda.time.DateTime
import services.MarriageAllowanceService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time

import scala.concurrent.{ExecutionContext, Future}

class FakeMarriageAllowanceService @Inject()( override val applicationConfig: ApplicationConfig,
                                              override val dataConnector: MarriageAllowanceDataConnector,
                                         override val emailConnector: EmailConnector,
                                         override val metrics: Metrics) extends MarriageAllowanceService(applicationConfig,
                                                                                                dataConnector,
                                                                                                emailConnector,
                                                                                                metrics) {

      override lazy val startTaxYear: Int = 2015
      val testingTime: DateTime = new DateTime(startTaxYear,1,1,0,0)
      override val currentTaxYear: Int = time.TaxYear.taxYearFor(testingTime.toLocalDate).startYear

      override def createMultiYearRelationship(createRelationshipRequestHolder: MultiYearCreateRelationshipRequestHolder,
                                               journey: String)
                                              (implicit hc: HeaderCarrier,
                                               ec: ExecutionContext): Future[Unit] = {
        super.createMultiYearRelationship(createRelationshipRequestHolder, journey)
      }

}
