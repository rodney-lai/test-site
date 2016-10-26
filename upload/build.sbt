name := """rodney-test-site-upload"""

version := "0.8.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-Xlint",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.48",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "ch.qos.logback" % "logback-core" % "1.1.7",
  "io.swagger" % "swagger-play2_2.11" % "1.5.3",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/stub/auth")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/stub/database")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/util")
