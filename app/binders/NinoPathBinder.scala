/*
 * Copyright 2023 HM Revenue & Customs
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

package binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.domain.Nino

import scala.util.{Failure, Success, Try}

object NinoPathBinder {
  implicit def pathBindable: PathBindable[Nino] =
    new PathBindable[Nino] {
      override def bind(key: String, value: String): Either[String, Nino] =
        Try(Nino(value)) match {
          case Success(value) =>
            Right(value)
          case Failure(error) =>
            Left(s"Cannot parse parameter '$key' with value '$value' as '${error.getMessage}'")
        }

      override def unbind(key: String, value: Nino): String =
        value.nino
    }
}
