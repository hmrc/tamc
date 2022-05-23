package test_utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

trait IntegrationSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(scaled(Span(5, Seconds)), scaled(Span(100, Millis)))
}
