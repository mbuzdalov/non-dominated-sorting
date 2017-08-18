lazy val commonSettings = Seq(
  organization := "ru.ifmo",
  libraryDependencies += junitInterface,
  autoScalaLibrary := false,
  crossPaths := false
)

lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test"
lazy val gson = "com.google.code.gson" % "gson" % "2.8.1"

lazy val root = project
  .in(file("."))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting",
            version := "0.0.0")
  .dependsOn(implementations, benchmarking)
  .aggregate(implementations, benchmarking)

lazy val implementations = project
  .in(file("implementations"))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting-implementations",
            version := "0.0.0")

lazy val benchmarking = project
  .in(file("benchmarking"))
  .settings(commonSettings :_*)
  .settings(name    := "non-dominated-sorting-benchmarking",
            version := "0.0.0",
            libraryDependencies += gson)
  .dependsOn(implementations)
  .enablePlugins(JmhPlugin)
