
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"   %% "domain"                      % "6.2.0-play-28",
    "uk.gov.hmrc"   %% "emailaddress"                % "3.5.0",
    "uk.gov.hmrc"   %% "bootstrap-backend-play-28"   % "5.18.0",
    "uk.gov.hmrc"   %% "tax-year"                    % "1.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"                % "jsoup"               % "1.13.1",
    "org.scalatestplus.play"  %% "scalatestplus-play"  % "5.1.0",
    "org.mockito"              % "mockito-core"        % "3.6.0",
    "com.github.tomakehurst"   % "wiremock-jre8"       % "2.27.0",
    "com.vladsch.flexmark"     % "flexmark-all"        % "0.35.10"
  ).map(_ % "test,it")

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
  )

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies
}
