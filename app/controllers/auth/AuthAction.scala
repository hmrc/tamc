/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import play.api.Logger
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector, val parser: BodyParsers.Default)
                              (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  private val logger: Logger = Logger(getClass)

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    authorised(ConfidenceLevel.L100) {
      Future.successful(None)
    }.recover {
      case t: Throwable =>
        logger.debug("Debug info - " + t.getMessage)
        Some(Unauthorized)
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[Request, AnyContent] with ActionFilter[Request]

class AuthConnector @Inject()(appConfig: ApplicationConfig, val http: HttpClient) extends PlayAuthConnector {

  lazy val serviceUrl: String = appConfig.AUTH_URL

}

