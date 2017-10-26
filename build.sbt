import sbt.Keys._

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

lazy val GatlingTest = config("gatling") extend Test

// This must be set to 2.11.11 because Gatling does not run on 2.12.2
scalaVersion in ThisBuild := "2.11.7"

libraryDependencies ++= Seq(
  guice,
  "org.joda" % "joda-convert" % "1.8",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.9",
  "com.typesafe.play" %% "play-json" % "2.6.0",
  "com.netaporter" %% "scala-uri" % "0.4.16",
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "org.web3j" % "core" % "2.3.1",
  "com.google.code.gson" % "gson" % "2.8.2",

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0-M3" % Test
)

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """blockchain-workbench""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

