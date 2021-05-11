package it

import org.scalatest.{Matchers, WordSpec}

class SmokeTestSpec extends WordSpec with Matchers {

  "2 + 2" should {

    "equal 4" in {

      2 + 2 shouldBe 4
    }
  }
}