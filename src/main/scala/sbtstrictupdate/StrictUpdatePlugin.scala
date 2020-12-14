package sbtstrictupdate

import sbt._
import sbt.Keys._
import sbt.librarymanagement.EvictionError

object StrictUpdatePlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = sbt.plugins.JvmPlugin

  object autoImport {
    val libraryDependencySchemes = settingKey[Seq[ModuleID]]("Versioning scheme to use for specific modules")
  }
  import autoImport._
  override lazy val globalSettings = Seq(
    libraryDependencySchemes := Nil,
  )
  override lazy val projectSettings = Seq(
    update := {
      val module = ivyModule.value
      val ur = updateFull.value
      val schemes = libraryDependencySchemes.value
      val evictionError = EvictionError(ur, module, schemes)
      evictionError.run
      ur
    }
  )
}
