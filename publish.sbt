ThisBuild / organization := "com.github.mbuzdalov"
ThisBuild / organizationName := "Maxim Buzdalov"
ThisBuild / organizationHomepage := Some(url("https://ctlab.itmo.ru/~mbuzdalov/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/mbuzdalov/non-dominated-sorting"),
    "scm:git@github.com:mbuzdalov/non-dominated-sorting.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "mbuzdalov",
    name  = "Maxim Buzdalov",
    email = "mbuzdalov@gmail.com",
    url   = url("https://ctlab.itmo.ru/~mbuzdalov")
  )
)

ThisBuild / description := "This repository contains implementations of algorithms for non-dominated sorting and a benchmarking suite."
ThisBuild / licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/mbuzdalov/non-dominated-sorting"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
