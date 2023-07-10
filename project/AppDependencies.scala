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

import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "7.19.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain"                    % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "emailaddress"              % "3.8.0",
    "uk.gov.hmrc"       %% "tax-year"                  % "3.2.0",
  )

  private def test(scope:Configuration = Test): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28" % bootstrapVersion,
    "org.mockito"              % "mockito-core"           % "5.2.0",
  ).map(_ % scope)

  private def it(scope:Configuration = IntegrationTest): Seq[ModuleID] = test(scope) ++ Seq(
    "com.github.tomakehurst" % "wiremock-jre8" % "2.30.1",
  ).map(_ % scope)

  def apply(): Seq[ModuleID] =
    compile ++ test() ++ it()
}
