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

import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "tamc"
val appPort = 9909
val appMajorVersion = 4
val appScalaVersion = "2.13.8"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    scalaSettings,
    defaultSettings(),
    majorVersion := appMajorVersion,
    scalaVersion := appScalaVersion,
    PlayKeys.playDefaultPort := appPort,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    routesImport ++= Seq("binders._", "uk.gov.hmrc.domain._"),
  )
  .configs(Compile)
  .settings(
    scalacOptions += "-deprecation",
    scalacOptions += "-feature",
    scalacOptions += "-Xfatal-warnings",
    // https://github.com/sbt/sbt/issues/6997
    // To resolve a bug with version 2.x.x of the scoverage plugin
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  )
  .settings(SilencerSettings())
  .configs(IntegrationTest)
  .settings(integrationTestSettings() *)
  .settings(CodeCoverageSettings() *)
