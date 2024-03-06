
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val hmrcMongoFeatureTogglesClientVersion  = "1.2.0"
  private val hmrcBootstrapVersion                  = "8.4.0"

  private val playVersion = "play-30"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion"            % hmrcBootstrapVersion,
    "uk.gov.hmrc"       %% s"domain-$playVersion"                       % "9.0.0",
    "uk.gov.hmrc"       %% s"emailaddress-$playVersion"                 % "4.0.0",
    "uk.gov.hmrc"       %% s"mongo-feature-toggles-client-$playVersion" % hmrcMongoFeatureTogglesClientVersion,
    "uk.gov.hmrc"       %% s"tax-year"                                  % "4.0.0",
    "org.typelevel"     %% "cats-core"                                  % "2.10.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"               % hmrcBootstrapVersion,
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
