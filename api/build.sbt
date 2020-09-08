import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.9.0"
ThisBuild / organization     := "com.rodneylai"
ThisBuild / organizationName := "rodneylai"

lazy val root = (project in file("."))
  .settings(
    name := "test-api",
    scalacOptions ++= Seq(
      "-Xlint:-byname-implicit,_",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings"
    ),
    libraryDependencies ++= Seq(
      Caliban.core,
      Caliban.http4s,
      Circe.core,
      Circe.generic,
      Circe.parser,
      jsoup,
      memcached,
      pureConfig,
      guice,
      Logging.Logback.core,
      Logging.Logback.classic,
      Logging.slf4j,
      scalaTest % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
