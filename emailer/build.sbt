name := """emailer"""

version := "v0.8.2"

scalaVersion := "2.11.8"

mainClass in assembly := Some("com.rodneylai.emailer")

scalacOptions ++= Seq(
  "-Xlint",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "net.debasishg" %% "redisclient" % "3.3",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "ch.qos.logback" % "logback-core" % "1.1.7",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.5",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.5",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.4",
  "com.google.inject" % "guice" % "4.1.0",
  "com.typesafe" % "config" % "1.3.1",
  "org.apache.commons" % "commons-email" % "1.4",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.mongodb.scala" % "mongo-scala-driver_2.11" % "1.2.1",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/database/common"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/util/common"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/util/console"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/auth/util"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/stub/auth"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/models/mongodb"
