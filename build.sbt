
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

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;config.ApplicationConfig;.*AuthService.*;models/.data/..*;view.*;app.*;prod.*;uk.gov.hmrc.BuildInfo;uk.gov.hmrc.play.*;connectors.ApplicationAuthConnector;connectors.ApplicationAuditConnector;config.ControllerConfiguration;errors.ErrorResponseStatus;metrics.*;config.*Filter;utils.*;models.RelationshipRecord;models.SendEmailRequest;models.RelationshipRecord;binders.*",
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
    scalaVersion := "2.13.8",
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
  "-Xfatal-warnings",
  "-Wconf:src=routes/.*:is,src=twirl/.*:is"
)

