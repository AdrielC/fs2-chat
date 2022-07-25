organization := "co.fs2"
name := "fs2-chat"

scalaVersion := "2.13.8"

resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-io" % "3.2.7",
  "co.fs2" %% "fs2-scodec" % "3.2.7",
  "org.jline" % "jline" % "3.12.1",
  "com.monovore" %% "decline" % "2.3.0",
  "org.typelevel" %% "squants" % "1.6.0",
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  "org.typelevel" %% "log4cats-slf4j"   % "2.4.0",  // Direct Slf4j Support - Recommended
)

fork in run := true
outputStrategy := Some(StdoutOutput)
connectInput in run := true

scalafmtOnCompile := false

addCompilerPlugin("org.scalameta" % "semanticdb-scalac_2.13.8" % "4.5.9")

scalacOptions ++= List(
  "-feature",
  "-language:higherKinds",
  "-Xlint",
  "-Yrangepos",
  "-Ywarn-unused"
)
scalafixDependencies in ThisBuild += "com.nequissimus" %% "sort-imports" % "0.6.1"

enablePlugins(UniversalPlugin, JavaAppPackaging)
