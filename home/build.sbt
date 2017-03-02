name := """rodney-test-site-home"""

version := "v0.8.2"

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
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.98",
  "com.github.mumoshu" % "play2-memcached-play24_2.11" % "0.7.0",
  "com.github.rjeschke" % "txtmark" % "0.13",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "be.objectify" %% "deadbolt-scala" % "2.5.0",
  "ch.qos.logback" % "logback-classic" % "1.2.1",
  "ch.qos.logback" % "logback-core" % "1.2.1",
  "jp.t2v" %% "play2-auth" % "0.14.2",
  "jp.t2v" %% "play2-auth-social" % "0.14.2",
  "jp.t2v" %% "play2-auth-test" % "0.14.2" % "test",
  "jp.t2v" %% "stackable-controller" % "0.5.1",
  "net.debasishg" %% "redisclient" % "3.3",
  "org.jsoup" % "jsoup" % "1.9.2",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.mongodb.scala" % "mongo-scala-driver_2.11" % "1.2.1",
  "org.slf4j" % "slf4j-api" % "1.7.24",
  "io.swagger" % "swagger-play2_2.11" % "1.5.3",
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

resolvers += "Maven" at "https://repo1.maven.org/maven2"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/auth")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/auth/util")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/models/mongodb")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/stackc")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/database/common")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/database/play")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/util/common")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/util/play")
