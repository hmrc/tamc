import sbt.{ModuleID, _}

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "domain" % "5.3.0",
    "uk.gov.hmrc" %% "emailaddress" % "3.2.0",
    "uk.gov.hmrc" %% "microservice-bootstrap" % "10.4.0",
    "uk.gov.hmrc" %% "tax-year" % "0.5.0"
  )
  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.5.0-play-25",
    "org.jsoup" % "jsoup" % "1.11.3",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
    "org.mockito" % "mockito-core" % "2.24.5"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}