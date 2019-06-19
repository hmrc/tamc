import sbt.{ModuleID, _}

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
    "uk.gov.hmrc" %% "emailaddress" % "3.2.0",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.40.0",
    "uk.gov.hmrc" %% "tax-year" % "0.5.0",
    "com.typesafe.play" %% "play-json-joda" % "2.6.13"
  )
  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26",
    "org.jsoup" % "jsoup" % "1.11.3",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2",
    "org.mockito" % "mockito-core" % "2.28.2"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
