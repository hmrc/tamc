package test_utils

import models.DesUpdateRelationshipRequest
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.http.logging.Authorization

case class HttpGETCallWithHeaders(url: String, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)
case class HttpPOSTCallWithHeaders(url: String, body: Any, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)
case class HttpPUTCallWithHeaders(url: String, body: DesUpdateRelationshipRequest, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)

class DummyHttpResponse(override val body: String, override val status: Int, override val allHeaders: Map[String, Seq[String]] = Map.empty) extends HttpResponse {
  override def json: JsValue = Json.parse(body)
}