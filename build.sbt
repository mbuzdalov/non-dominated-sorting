lazy val commonSettings = Seq(
  organization := "ru.ifmo",
  libraryDependencies += junitInterface,
  autoScalaLibrary := false,
  crossPaths := false
)

lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test"

lazy val root = project
  .in(file("."))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting",
            version := "0.0.0")
  .dependsOn(implementations, benchmarking, analysis)
  .aggregate(implementations, benchmarking, analysis)

lazy val implementations = project
  .in(file("implementations"))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting-implementations",
            version := "0.0.0")

lazy val benchmarking = project
  .in(file("benchmarking"))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting-benchmarking",
            version := "0.0.0")
  .dependsOn(implementations)
  .enablePlugins(JmhPlugin)

lazy val analysis = project
  .in(file("analysis"))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting-analysis",
            version := "0.0.0")
