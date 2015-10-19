name := """rodney-test-site-home"""

version := "v0.8.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

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
  "be.objectify" %% "deadbolt-scala" % "2.4.1.1",
  "jp.t2v" %% "play2-auth" % "0.14.1",
  "jp.t2v" %% "play2-auth-social" % "0.14.1",
  "jp.t2v" %% "play2-auth-test" % "0.14.1" % "test",
  "jp.t2v" %% "stackable-controller" % "0.5.0",
  "org.jsoup" % "jsoup" % "1.8.3",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.mongodb" % "casbah_2.11" % "2.8.2",
  "pl.matisoft" %% "swagger-play24" % "1.4",
  "log4j" % "log4j" % "1.2.17",
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

(unmanagedSourceDirectories in Compile) <+= baseDirectory(_ / "../lib/org/mindrot/jbcrypt")

