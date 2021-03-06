name := """rodney-test-site-upload"""

maintainer := "rlai@irismedia.com"

version := "0.9.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.3"

scalacOptions ++= Seq(
  "-Xlint",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

// hack to suppress unused import errors/warnings
routesImport := Seq.empty
TwirlKeys.templateImports := Seq.empty

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

// Determine current platform
val platform = {
  // Determine platform name using code similar to javacpp
  // com.googlecode.javacpp.Loader.java line 60-84
  val jvmName = System.getProperty("java.vm.name").toLowerCase
  var osName = System.getProperty("os.name").toLowerCase
  var osArch = System.getProperty("os.arch").toLowerCase
  if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
    osName = "android"
  } else if (jvmName.startsWith("robovm") && osName.startsWith("darwin")) {
    osName = "ios"
    osArch = "arm"
  } else if (osName.startsWith("mac os x")) {
    osName = "macosx"
  } else {
    val spaceIndex = osName.indexOf(' ')
    if (spaceIndex > 0) {
      osName = osName.substring(0, spaceIndex)
    }
  }
  if (osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586") || osArch.equals("i686")) {
    osArch = "x86"
  } else if (osArch.equals("amd64") || osArch.equals("x86-64") || osArch.equals("x64")) {
    osArch = "x86_64"
  } else if (osArch.startsWith("arm")) {
    osArch = "arm"
  }
  val platformName = osName + "-" + osArch
  println("platform: " + platformName)
  platformName
}

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.854",
  "com.typesafe.play" %% "play-mailer-guice" % "8.0.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "com.iterable" %% "swagger-play" % "2.0.1",
  "org.bytedeco" % "opencv-platform" % "4.4.0-1.5.4",
  "org.slf4j" % "slf4j-api" % "1.7.30",
  jdbc,
  ehcache,
  guice,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/stub/auth"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/stub/database"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/common"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util" / ("scala_" + scalaBinaryVersion.value)

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/play"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/play_2.x"
