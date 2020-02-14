/*
 * Copyright 2015 HM Revenue & Customs
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

import sbt._
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "domain"                 % "5.6.0-play-25",
    "uk.gov.hmrc" %% "emailaddress"           % "3.2.0",
    "uk.gov.hmrc" %% "microservice-bootstrap" % "10.4.0",
    "uk.gov.hmrc" %% "tax-year"               % "0.6.0",
    "uk.gov.hmrc"  %% "auth-client"          % "2.31.0-play-25"
  )

  val test: Seq[ModuleID] = Seq(
      "uk.gov.hmrc"             %% "hmrctest"           % "3.9.0-play-25",
      "org.jsoup"                % "jsoup"              % "1.11.3",
      "org.scalatestplus.play"  %% "scalatestplus-play" % "2.0.1",
      "org.mockito"              % "mockito-core"       % "2.24.5",
      "com.github.tomakehurst"   % "wiremock"           % "2.26.0"
    ).map(_ % "test")


  val all: Seq[ModuleID] = compile ++ test
}
