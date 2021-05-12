package test_utils

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

trait IntegrationSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(scaled(Span(5, Seconds)), scaled(Span(100, Millis)))
}