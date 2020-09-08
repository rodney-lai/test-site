name := """rodney-test-site-home"""

version := "v0.9.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

scalacOptions ++= Seq(
  "-Xlint",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.851",
  "com.github.mumoshu" %% "play2-memcached-play25" % "0.8.0",
  "com.github.rjeschke" % "txtmark" % "0.13",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "be.objectify" %% "deadbolt-scala" % "2.5.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "jp.t2v" %% "play2-auth" % "0.14.2",
  "net.debasishg" %% "redisclient" % "3.30",
  "org.jsoup" % "jsoup" % "1.13.1",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.0.4",
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "io.swagger" %% "swagger-play2" % "1.5.3",
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % Test
)

resolvers += "Maven" at "https://repo1.maven.org/maven2"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/amazon/s3"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/auth"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/auth/util"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/models/mongodb"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/stackc"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/database/common"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/database/play"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/common"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/util" / ("scala_" + scalaBinaryVersion.value)

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/play"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/play_2.5"
