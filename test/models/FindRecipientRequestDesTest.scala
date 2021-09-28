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

package models

import uk.gov.hmrc.domain.Generator
import test_utils.UnitSpec

class FindRecipientRequestDesTest extends UnitSpec {

  "FindRecipientRequestDes" should {

    "return a FindRecipientRequestDes given an instance of FindRecipientRequest" in {

      val name = "testName"
      val lastName = "testLastName"
      val genderMale = "M"
      val gender = Gender(genderMale)
      val generatedNino = new Generator().nextNino

      val findRecipientRequest = FindRecipientRequest(name, lastName, gender, generatedNino)
      val expectedResult = FindRecipientRequestDes(surname = lastName, forename1 = name, forename2 = None, gender = Some(genderMale))

      FindRecipientRequestDes(findRecipientRequest) shouldBe expectedResult

    }

  }

}
