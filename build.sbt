import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "tamc"

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 4
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Xfatal-warnings",
  "-Wconf:src=routes/.*:is,src=twirl/.*:is"
)

val scoverageSettings: Seq[Def.Setting[?]] = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;config.ApplicationConfig;.*AuthService.*;models/.data/..*;view.*;app.*;prod.*;uk.gov.hmrc.BuildInfo;uk.gov.hmrc.play.*;connectors.ApplicationAuthConnector;connectors.ApplicationAuditConnector;config.ControllerConfiguration;errors.ErrorResponseStatus;metrics.*;config.*Filter;utils.*;models.RelationshipRecord;models.SendEmailRequest;models.RelationshipRecord;binders.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    defaultSettings(),
    scalaSettings,
    scoverageSettings,
    PlayKeys.playDefaultPort := 9909,
    retrieveManaged := true,
    libraryDependencies ++= AppDependencies.all,
    routesImport ++= Seq("binders._", "uk.gov.hmrc.domain._")
  )

val it: Project = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
