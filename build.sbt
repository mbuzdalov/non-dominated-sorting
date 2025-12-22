lazy val commonSettings = Seq(
  organization := "com.github.mbuzdalov",
  libraryDependencies += junitInterface,
  autoScalaLibrary := false,
  crossPaths := false,
  fork := true
)

lazy val junitInterface = "com.github.sbt" % "junit-interface" % "0.13.3" % "test"
lazy val osHardwareInformation = "com.github.oshi" % "oshi-core" % "6.9.2"

lazy val root = project
  .in(file("."))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting",
            version := "0.2.1")
  .dependsOn(implementations, benchmarking)
  .aggregate(implementations, benchmarking)

lazy val implementations = project
  .in(file("implementations"))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting-implementations",
            version := "0.2.1")

lazy val benchmarking = project
  .in(file("benchmarking"))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting-benchmarking",
            version := "0.2.1",
            libraryDependencies += osHardwareInformation)
  .dependsOn(implementations)
  .enablePlugins(JmhPlugin)
