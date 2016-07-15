package test_utils

import models.{DesUpdateRelationshipRequest, SendEmailRequest}
import play.api.libs.json.Writes
import test_utils.TestData._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.Authorization

import scala.concurrent.Future

case class HttpGETCallWithHeaders(url: String, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)
case class HttpPOSTCallWithHeaders(url: String, body: Any, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)
case class HttpPUTCallWithHeaders(url: String, body: DesUpdateRelationshipRequest, env: Seq[(String, String)] = Seq(), bearerToken: Option[Authorization] = None)

object FakeHttpVerbs {

  val fakeHttpGet = new HttpGet {
    override val hooks = NoneRequired
    var httpGetCallsToTest: List[HttpGETCallWithHeaders] = List()
    override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
      val adjustedUrl = s"GET-${url}"
      httpGetCallsToTest = httpGetCallsToTest :+ HttpGETCallWithHeaders(url = adjustedUrl, env = hc.extraHeaders, bearerToken = hc.authorization)
      var responseBody = findMockData(adjustedUrl)
      val response = new DummyHttpResponse(responseBody, 200)
      return Future.successful(response)
    }
  }

  val fakeHttpPost = new HttpPost {
    override val hooks = NoneRequired
    var httpPostCallsToTest: List[HttpPOSTCallWithHeaders] = List()
    protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
      val adjustedUrl = s"POST-${url}"
      httpPostCallsToTest = httpPostCallsToTest :+ HttpPOSTCallWithHeaders(url = adjustedUrl, body = body, env = hc.extraHeaders, bearerToken = hc.authorization)
      var responseBody = findMockData(adjustedUrl, Some(body))
      val response = new DummyHttpResponse(responseBody, 200)
      Future.successful(response)
    }
    protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
  }

  val fakeHttpPut = new HttpPut {
    override val hooks = NoneRequired
    var httpPutCallsToTest: List[HttpPUTCallWithHeaders] = List()
    protected def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[uk.gov.hmrc.play.http.HttpResponse] = {
      val adjustedUrl = s"PUT-${url}"
      val adjustedBody = body.asInstanceOf[DesUpdateRelationshipRequest]
      httpPutCallsToTest = httpPutCallsToTest :+ HttpPUTCallWithHeaders(url = adjustedUrl, body = adjustedBody, env = hc.extraHeaders, bearerToken = hc.authorization)
      var responseBody = findMockData(adjustedUrl, Some(adjustedBody))
      val response = new DummyHttpResponse(responseBody, 200)
      Future.successful(response)
    }

    protected def doPutString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    protected def doEmptyPut[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    protected def doFormPut(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
  }

  val fakeHttpEmailPost = new HttpPost {
    override val hooks = NoneRequired

    var checkEmailCallCount = 0
    var checkEmailCallData: Option[SendEmailRequest] = None

    protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
      checkEmailCallCount = checkEmailCallCount + 1
      checkEmailCallData = Some(body.asInstanceOf[SendEmailRequest])
      body.asInstanceOf[SendEmailRequest] match {
        case SendEmailRequest(addressList, _, _, _) if (addressList.exists { address => address.value.equals("bad-request@example.com") }) =>
          Future.failed(new BadRequestException("throwing error for:" + addressList))
        case _ =>
          Future.successful(new DummyHttpResponse("", 200))
      }
    }
    protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
    protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
  }

}
