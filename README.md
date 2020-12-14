sbt-strict-update
=================

sbt-strict-update enforces versionScheme policy for select modules.

setup
-----

Add this to `project/plugins.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-strict-update" % "0.1.0")
```

usage
-----

Add the following to your `build.sbt`

```scala
ThisBuild / libraryDependencySchemes += "org.typelevel" %% "cats-effect" % "semver-early"
```
