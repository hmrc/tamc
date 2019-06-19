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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.Inject
import models.DesUpdateRelationshipRequest
import play.api.{Configuration, Play}
import play.api.libs.json.Writes
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.DummyHttpResponse
import utils.TestData._

import scala.concurrent.Future

class FakeHttpClient @Inject()(config: Configuration,
                               actorSys: ActorSystem) extends HttpClient {

  override val hooks: Seq[HttpHook] = Seq.empty[HttpHook]

  override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val adjustedUrl = s"PUT-$url"
        val adjustedBody = body.asInstanceOf[DesUpdateRelationshipRequest]
        val responseBody = findMockData(adjustedUrl, Some(adjustedBody))
        val response = new DummyHttpResponse(responseBody, 200)
        Future.successful(response)
  }

  override def doPutString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
      val adjustedUrl = s"GET-$url"
      val responseBody = findMockData(adjustedUrl)
      val response = new DummyHttpResponse(responseBody, 200)
      Future.successful(response)
  }

  override def doPatch[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        val adjustedUrl = s"POST-$url"
        val responseBody = findMockData(adjustedUrl, Some(body))
        val response = new DummyHttpResponse(responseBody, 200)
        Future.successful(response)
}

  override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def actorSystem: ActorSystem = actorSys
  override protected def configuration: Option[Config] = Some(config.underlying)
}
