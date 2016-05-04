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
import play.PlayImport.PlayKeys._
import play.core.PlayVersion

object MicroServiceBuild extends Build with MicroService {

  val appName = "tamc"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
  override lazy val playSettings = Seq(routesImport ++= Seq("binders._", "uk.gov.hmrc.domain._"))
}

private object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "domain" % "3.3.0",
    "uk.gov.hmrc" %% "emailaddress" % "1.1.0",
    "uk.gov.hmrc" %% "http-verbs" % "3.3.0",
    "uk.gov.hmrc" %% "microservice-bootstrap" % "4.2.0",
    "uk.gov.hmrc" %% "play-authorisation" % "3.1.0",
    "uk.gov.hmrc" %% "play-config" % "2.0.1",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.0",
    "uk.gov.hmrc" %% "play-url-binders" % "1.0.0",
    "uk.gov.hmrc" %% "time" % "2.1.0",
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8")

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "org.scalatest" %% "scalatest" % "2.2.6" % scope)
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
