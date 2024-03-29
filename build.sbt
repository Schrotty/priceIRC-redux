import sbt.Keys.scalacOptions

name := "priceIRC-redux"
version := "1.0"
scalaVersion := "2.13.2"
libraryDependencies ++= Seq(

  //ANTLR
  "org.antlr" % "ST4" % "4.3.1",
  "org.antlr" % "antlr4-runtime" % "4.8",
  "org.antlr" % "stringtemplate" % "3.2",

  //AKKA
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.7",
  "com.typesafe.akka" %% "akka-stream" % "2.6.7",

  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalactic" %% "scalactic" % "3.2.0" % "test",
  "org.scalatest" % "scalatest_2.13" % "3.2.0",
  "commons-lang" % "commons-lang" % "2.6",
  "com.lihaoyi" %% "os-lib" % "0.6.2",

  //LOGGING
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "12.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.13.0" % Runtime,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

  "org.fusesource.jansi" % "jansi" % "1.18",

  "org.scala-lang.modules" %% "scala-async" % "0.10.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
)

// PLUGINS AND SETTINGS
enablePlugins(Antlr4Plugin)
antlr4PackageName in Antlr4 := Some("de.rubenmaurer.price.antlr4")
antlr4Version in Antlr4 := "4.9.1"

enablePlugins(JavaAppPackaging)
assemblyJarName in assembly := "priceIRC.jar"

inThisBuild(
  List(
    scalaVersion := scalaVersion.value,
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
    scalafixScalaBinaryVersion := "2.13"
  )
)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber),
    buildInfoPackage := "de.rubenmaurer.price.util"
  )