
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "domain"                    % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "emailaddress"              % "3.7.0",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "7.12.0",
    "uk.gov.hmrc"       %% "tax-year"                  % "3.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"                % "jsoup"              % "1.15.3",
    "org.scalatestplus.play"  %% "scalatestplus-play" % "5.1.0",
    "org.mockito"              % "mockito-core"       % "4.11.0",
    "com.github.tomakehurst"   % "wiremock-jre8"      % "2.30.1",
    "com.vladsch.flexmark"     % "flexmark-all"       % "0.36.8"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}
