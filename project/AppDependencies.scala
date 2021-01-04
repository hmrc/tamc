
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "domain"                     % "5.10.0-play-26",
    "uk.gov.hmrc" %% "emailaddress"               % "3.5.0",
    "uk.gov.hmrc" %% "bootstrap-backend-play-26"  % "3.2.0",
    "uk.gov.hmrc" %% "tax-year"                   % "1.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "hmrctest"           % "3.10.0-play-26",
    "org.jsoup"                % "jsoup"              % "1.13.1",
    "org.scalatestplus.play"  %% "scalatestplus-play" % "5.1.0",
    "org.mockito"              % "mockito-core"       % "3.6.0",
    "com.github.tomakehurst"   % "wiremock"           % "2.27.2"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
