name := """rodney-test-site-upload"""

version := "0.8.3"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

scalacOptions ++= Seq(
  "-Xlint",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

val javacvVersion = "1.3"

val javacppVersion = "1.3"

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
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.773",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "io.swagger" % "swagger-play2_2.11" % "1.5.3",
  "org.bytedeco" % "javacv" % javacvVersion excludeAll(
    ExclusionRule(organization = "org.bytedeco.javacpp-presets"),
    ExclusionRule(organization = "org.bytedeco.javacpp")
    ),
  "org.bytedeco.javacpp-presets" % "opencv"  % ("3.1.0-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv"  % ("3.1.0-" + javacppVersion) classifier platform,
  "org.bytedeco" % "javacpp" % javacppVersion,
  "org.slf4j" % "slf4j-api" % "1.7.30",
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/stub/auth"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/stub/database"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/common"

(unmanagedSourceDirectories in Compile) += baseDirectory.value / "../lib/com/rodneylai/util/play"
