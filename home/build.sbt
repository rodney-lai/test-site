name := """rodney-test-site-home"""

version := "v0.8.1"

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
  "com.github.mumoshu" % "play2-memcached-play24_2.11" % "0.7.0",
  "com.github.rjeschke" % "txtmark" % "0.13",
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "be.objectify" %% "deadbolt-scala" % "2.4.3",
  "ch.qos.logback" % "logback-classic" % "1.1.6",
  "ch.qos.logback" % "logback-core" % "1.1.6",
  "jp.t2v" %% "play2-auth" % "0.14.2",
  "jp.t2v" %% "play2-auth-social" % "0.14.2",
  "jp.t2v" %% "play2-auth-test" % "0.14.2" % "test",
  "jp.t2v" %% "stackable-controller" % "0.5.1",
  "org.jsoup" % "jsoup" % "1.8.3",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.mongodb.scala" % "mongo-scala-driver_2.11" % "1.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.18",
  "pl.matisoft" %% "swagger-play24" % "1.4",
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "Maven" at "https://repo1.maven.org/maven2"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/auth")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/models/mongodb")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/stackc")

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/com/rodneylai/util")
