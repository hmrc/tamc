import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.{Resolver, _}
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}

val appName = "tamc"

lazy val plugins: Seq[Plugins] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;config.ApplicationConfig;.*AuthService.*;models/.data/..*;view.*;app.*;prod.*;uk.gov.hmrc.BuildInfo;uk.gov.hmrc.play.*;connectors.ApplicationAuthConnector;connectors.ApplicationAuditConnector;config.ControllerConfiguration;errors.ErrorResponseStatus;metrics.*;config.*Filter;utils.*;models.RelationshipRecord;models.SendEmailRequest",
    ScoverageKeys.coverageMinimum := 74,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(scoverageSettings,
    majorVersion := 4,
    PlayKeys.playDefaultPort := 9909,
    resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases")),
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies.all,
    parallelExecution in Test := false,
    fork in Test := true,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    routesImport ++= Seq("binders._", "uk.gov.hmrc.domain._")
  )
