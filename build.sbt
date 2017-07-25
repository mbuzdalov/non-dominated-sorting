lazy val commonSettings = Seq(
  organization := "ru.ifmo",
  scalaVersion := "2.12.1",
  libraryDependencies += junitInterface
)

lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test"

lazy val root = project.
  in(file(".")).
  settings(commonSettings :_*).
  settings(name    := "non-dominated-sorting",
           version := "0.0.0").
  dependsOn(implementations, benchmarking).
  aggregate(implementations, benchmarking)

lazy val implementations = project.
  in(file("implementations")).
  settings(commonSettings :_*).
  settings(name    := "non-dominated-sorting-implementations",
           version := "0.0.0")

lazy val benchmarking = project.
  in(file("benchmarking")).
  settings(commonSettings :_*).
  settings(name    := "non-dominated-sorting-benchmarking",
           version := "0.0.0").
  dependsOn(implementations).
  enablePlugins(JmhPlugin)
