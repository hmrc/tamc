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

import connectors.{EmailConnector, MarriageAllowanceDataConnector}
import errors.ErrorResponseStatus.BAD_REQUEST
import javax.inject.Inject
import metrics.Metrics
import models.MultiYearCreateRelationshipRequestHolder
import services.MarriageAllowanceService
import utils.TestData.Cids
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.time

import scala.concurrent.{ExecutionContext, Future}

class FakeMarriageAllowanceErrrorControllerService @Inject()(override val dataConnector: MarriageAllowanceDataConnector,
                                                             override val emailConnector: EmailConnector,
                                                             override val metrics: Metrics) extends MarriageAllowanceService(dataConnector,
  emailConnector,
  metrics) {

  override val currentTaxYear: Int = time.TaxYear.taxYearFor(testingTime.toLocalDate).startYear
  override val startTaxYear = 2015
  override val maSupportedYearsCount = 5

  override def createMultiYearRelationship(createRelationshipRequestHolder: MultiYearCreateRelationshipRequestHolder,
                                           journey: String)
                                          (implicit hc: HeaderCarrier,
                                           ec: ExecutionContext): Future[Unit] = {
    createRelationshipRequestHolder.request.transferor_cid match {
      case Cids.cidBadRequest => throw new BadRequestException(BAD_REQUEST)
      case Cids.cidConflict => Future.failed(Upstream4xxResponse("Cannot update as Participant", 409, 409))
      case Cids.cidServiceUnavailable => Future.failed(Upstream5xxResponse("LTM000503", 503, 503))
      case _ => throw new Exception("this exception should not be thrown")
    }
  }

}
