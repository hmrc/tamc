
import sbt.*

object AppDependencies {

  private val hmrcBootstrapVersion = "9.5.0"
  private val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-backend-$playVersion"  % hmrcBootstrapVersion,
    "uk.gov.hmrc"   %% s"domain-$playVersion"             % "10.0.0",
    "uk.gov.hmrc"   %% s"tax-year"                        % "5.0.0",
    "org.typelevel" %% "cats-core"                        % "2.12.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-test-$playVersion"    % hmrcBootstrapVersion,
    "org.scalatestplus"   %% "scalacheck-1-17"                 % "3.2.18.0",
    "org.scalacheck"      %% "scalacheck"                      % "1.18.1"
  ) map (_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
