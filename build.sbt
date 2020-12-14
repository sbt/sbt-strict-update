ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.eed3si9n"
ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-eviction-error"))
ThisBuild / description := "sbt plugin to enforce stricter version scheme policy"
ThisBuild / licenses := Seq("MIT License" -> url("https://opensource.org/licenses/MIT"))

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-strict-update",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.4.4"
      }
    },
    scriptedBufferLog := false,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
    },
    publishMavenStyle := false,
    bintrayOrganization := None,
    bintrayRepository := "sbt-plugins",
  )
