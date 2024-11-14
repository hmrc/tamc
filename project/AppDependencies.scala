
import sbt.*

object AppDependencies {

  private val hmrcBootstrapVersion = "9.2.0"

  private val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-backend-$playVersion"  % hmrcBootstrapVersion,
    "uk.gov.hmrc"   %% s"domain-$playVersion"             % "10.0.0",
    "uk.gov.hmrc"   %% s"emailaddress-$playVersion"       % "4.0.0",
    "uk.gov.hmrc"   %% s"tax-year"                        % "5.0.0",
    "org.typelevel" %% "cats-core"                        % "2.12.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-test-$playVersion"     % hmrcBootstrapVersion
  ) map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
