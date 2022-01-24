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

import com.google.inject.Inject
import config.ApplicationConfig
import models.SendEmailRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject()(http: HttpClient, appConfig: ApplicationConfig)(implicit val ec: ExecutionContext) {

  val emailUrl = appConfig.EMAIL_URL

  def url(path: String) = s"$emailUrl$path"

  def sendEmail(sendEmailRequest: SendEmailRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.POST(url("/hmrc/email"), sendEmailRequest)
}
