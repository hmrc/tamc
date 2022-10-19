
import sbt._
import sbt.Keys._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.SbtAutoBuildPlugin

val appName = "tamc"

lazy val plugins: Seq[Plugins] = Seq.empty

val suppressedImports = Seq("-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlFeatureImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlHelperImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Html",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.JavaScript",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Txt",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Xml",
  "-P:silencer:lineContentFilters=import models._",
  "-P:silencer:lineContentFilters=import controllers._",
  "-P:silencer:lineContentFilters=import play.api.i18n._",
  "-P:silencer:lineContentFilters=import views.html._",
  "-P:silencer:lineContentFilters=import play.api.templates.PlayMagic._",
  "-P:silencer:lineContentFilters=import play.api.mvc._",
  "-P:silencer:lineContentFilters=import play.api.data._")

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;config.ApplicationConfig;.*AuthService.*;models/.data/..*;view.*;app.*;prod.*;uk.gov.hmrc.BuildInfo;uk.gov.hmrc.play.*;connectors.ApplicationAuthConnector;connectors.ApplicationAuditConnector;config.ControllerConfiguration;errors.ErrorResponseStatus;metrics.*;config.*Filter;utils.*;models.RelationshipRecord;models.SendEmailRequest",
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(scoverageSettings,
    majorVersion := 4,
    PlayKeys.playDefaultPort := 9909,
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesImport ++= Seq("binders._", "uk.gov.hmrc.domain._")
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false
  )
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-P:silencer:pathFilters=routes",
  "-Xfatal-warnings"
)
scalacOptions ++= suppressedImports
