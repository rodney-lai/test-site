import sbt._

object Dependencies {

  object Caliban {
    val version = "0.9.1"
    lazy val core = "com.github.ghostdogpr" %% "caliban" % version
    lazy val http4s = "com.github.ghostdogpr" %% "caliban-http4s" % version
  }

  object Logging {
    object Logback {
      val version = "1.2.3"
      lazy val classic = "ch.qos.logback" % "logback-classic" % version
      lazy val core = "ch.qos.logback" % "logback-core" % version
    }
    lazy val slf4j = "org.slf4j" % "slf4j-api" % "1.7.30"
  }

  object Circe {
    val version = "0.13.0"

    lazy val core = "io.circe" %% "circe-core" % version
    lazy val generic = "io.circe" %% "circe-generic" % version
    lazy val parser = "io.circe" %% "circe-parser" % version
  }

  lazy val guice = "com.google.inject" % "guice" % "4.2.3"

  lazy val memcached = "net.spy" % "spymemcached" % "2.12.3"

  lazy val jsoup = "org.jsoup" % "jsoup" % "1.13.1"

  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.13.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
}
