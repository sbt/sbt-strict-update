ThisBuild / scalaVersion := "2.13.3"

ThisBuild / libraryDependencySchemes += "org.typelevel" %% "cats-effect" % "early-semver"

lazy val root = (project in file("."))
  .settings(
    name := "demo",
    libraryDependencies ++= List(
      "org.http4s" %% "http4s-blaze-server" % "0.21.11",
      "org.typelevel" %% "cats-effect" % "3.0-8096649",
    ),
  )