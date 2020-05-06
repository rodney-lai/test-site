name := """emailer"""

version := "v0.8.3"

scalaVersion := "2.11.12"

mainClass in assembly := Some("com.rodneylai.emailer")

scalacOptions ++= Seq(
  "-Xlint",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.773",
  "net.debasishg" %% "redisclient" % "3.6",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "com.typesafe.slick" %% "slick-codegen" % "3.2.3",
  "org.postgresql" % "postgresql" % "9.4.1212",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.9.5",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.5",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.9.5",
  "com.google.inject" % "guice" % "4.1.0",
  "com.typesafe" % "config" % "1.3.3",
  "org.apache.commons" % "commons-email" % "1.5",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.0.2",
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/amazon/s3"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/database/common"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/util/common"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/util/console"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/auth/util"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/stub/auth"

unmanagedSourceDirectories in Compile += baseDirectory.value / "../lib/com/rodneylai/models/mongodb"

lazy val slickCodeGen = taskKey[Seq[File]]("gen-tables")

slickCodeGen := {
  val outputDir = (sourceManaged.value / "slick").getPath // place generated files in sbt's managed sources folder
  val url = "jdbc:postgresql://localhost:5432/codegen?user=postgres&password=postgres"
  val jdbcDriver = "org.postgresql.Driver"
  val slickDriver = "slick.jdbc.PostgresProfile"
  val pkg = "com.rodneylai.database"
  toError((runner in Compile).value.run("slick.codegen.SourceCodeGenerator", (dependencyClasspath in Compile).value.files, Array(slickDriver, jdbcDriver, url, outputDir, pkg), streams.value.log))
  val fname = outputDir + "/com/rodneylai/database/Tables.scala"
  Seq(file(fname))
}
