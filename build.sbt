import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "tamc"

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / majorVersion := 4
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Xfatal-warnings",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:msg=Flag.*repeatedly:s",
  "-Wconf:msg=.*-Wunused.*:s"
)

val scoverageSettings: Seq[Def.Setting[?]] = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedFiles := ";.*Routes.*;RoutesPrefix.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageMinimumBranchTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    defaultSettings(),
    scalaSettings,
    scoverageSettings,
    PlayKeys.playDefaultPort := 9909,
    retrieveManaged := true,
    libraryDependencies ++= AppDependencies.all,
    routesImport ++= Seq("binders.NinoPathBinder._", "uk.gov.hmrc.domain._")
  )

val it: Project = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
