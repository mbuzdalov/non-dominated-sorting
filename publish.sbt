organization in ThisBuild := "com.github.mbuzdalov"
organizationName in ThisBuild := "Maxim Buzdalov"
organizationHomepage in ThisBuild := Some(url("https://ctlab.itmo.ru/~mbuzdalov/"))

scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/mbuzdalov/non-dominated-sorting"),
    "scm:git@github.com:mbuzdalov/non-dominated-sorting.git"
  )
)
developers in ThisBuild  := List(
  Developer(
    id    = "mbuzdalov",
    name  = "Maxim Buzdalov",
    email = "mbuzdalov@gmail.com",
    url   = url("https://ctlab.itmo.ru/~mbuzdalov")
  )
)

description in ThisBuild := "This repository contains implementations of algorithms for non-dominated sorting and a benchmarking suite."
licenses in ThisBuild := List("MIT" -> new URL("https://opensource.org/licenses/MIT"))
homepage in ThisBuild := Some(url("https://github.com/mbuzdalov/non-dominated-sorting"))

// Remove all additional repository other than Maven Central from POM
pomIncludeRepository in ThisBuild := { _ => false }
publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle in ThisBuild := true
