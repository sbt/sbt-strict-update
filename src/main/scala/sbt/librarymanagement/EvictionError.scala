package sbt
package librarymanagement

import scala.collection.mutable
import sbt.internal.librarymanagement.VersionSchemes

object EvictionError {
  def apply(
    report: UpdateReport,
    module: ModuleDescriptor,
    schemes: Seq[ModuleID],
  ): EvictionError = {
    val options = EvictionWarningOptions.full
    val evictions = EvictionWarning.buildEvictions(options, report)
    processEvictions(module, options, evictions, schemes)
  }
  private[sbt] def processEvictions(
      module: ModuleDescriptor,
      options: EvictionWarningOptions,
      reports: Seq[OrganizationArtifactReport],
      schemes: Seq[ModuleID],
  ): EvictionError = {
    val directDependencies = module.directDependencies
    val pairs = reports map { detail =>
      val evicteds = detail.modules filter { _.evicted }
      val winner = (detail.modules filterNot { _.evicted }).headOption
      new EvictionPair(
        detail.organization,
        detail.name,
        winner,
        evicteds,
        true,
        options.showCallers
      )
    }
    val incompatibleEvictions: mutable.ListBuffer[(EvictionPair, String)] = mutable.ListBuffer()
    var binaryIncompatibleEvictionExists = false
    val assumeTrue
        : PartialFunction[(ModuleID, Option[ModuleID], Option[ScalaModuleInfo]), Boolean] = {
      case (_, _, _) => true
    }
    val sbvOpt = module.scalaModuleInfo.map(_.scalaBinaryVersion)
    val schemeMap: Map[(String, String), String] = Map(schemes map { s =>
      val organization = s.organization
      val name =
        if (s.crossVersion == CrossVersion.binary && sbvOpt.isDefined) s.name + "_" + sbvOpt.get
        else s.name
      VersionSchemes.validateScheme(s.revision)
      val versionScheme = s.revision
      (organization, name) -> versionScheme
    }: _*)
    def calculateCompatible(p: EvictionPair): (Boolean, String) = {
      val winnerOpt = p.winner map { _.module }
      val extraAttributes = (p.winner match {
        case Some(r) => r.extraAttributes
        case _       => Map.empty
      }) ++ (winnerOpt match {
        case Some(w) => w.extraAttributes
        case _       => Map.empty
      })
      val schemeOpt = VersionSchemes.extractFromExtraAttributes(extraAttributes)
        .orElse(schemeMap.get((p.organization, p.name)))
      val f = (winnerOpt, schemeOpt) match {
        case (Some(_), Some(VersionSchemes.EarlySemVer)) =>
          EvictionWarningOptions.guessEarlySemVer
        case (Some(_), Some(VersionSchemes.SemVerSpec)) =>
          EvictionWarningOptions.guessSemVer
        case (Some(_), Some(VersionSchemes.PackVer)) =>
          EvictionWarningOptions.guessSecondSegment
        case _ => assumeTrue
      }
      (p.evicteds forall { r =>
        f((r.module, winnerOpt, module.scalaModuleInfo))
      }, schemeOpt.getOrElse("?"))
    }
    pairs foreach {
      case p if p.winner.isDefined =>
        // don't report on a transitive eviction that does not have a winner
        // https://github.com/sbt/sbt/issues/4946
        if (p.winner.isDefined) {
          val r = calculateCompatible(p)
          if (!r._1) {
            incompatibleEvictions += (p -> r._2)
            binaryIncompatibleEvictionExists = true
          }
        }
      case _ => ()
    }
    new EvictionError(
      incompatibleEvictions.toList,
    )
  }

  def toLines(error: EvictionError): List[String] = {
    val out: mutable.ListBuffer[String] = mutable.ListBuffer()
    out += "found version conflict(s) in library dependencies; some are suspected to be binary incompatible:"
    out += ""
    error.incompatibleEvictions.foreach({
      case (a, scheme) =>
        val revs = a.evicteds map { _.module.revision }
        val revsStr = if (revs.size <= 1) revs.mkString else "{" + revs.mkString(", ") + "}"
        val seen: mutable.Set[ModuleID] = mutable.Set()
        val callers: List[String] = (a.evicteds.toList ::: a.winner.toList) flatMap { r =>
          val rev = r.module.revision
          r.callers.toList flatMap { caller =>
            if (seen(caller.caller)) Nil
            else {
              seen += caller.caller
              List(f"\t    +- ${caller}%-50s (depends on $rev)")
            }
          }
        }
        val winnerRev = a.winner match {
          case Some(r) => s":${r.module.revision} ($scheme) is selected over ${revsStr}"
          case _       => " is evicted for all versions"
        }
        val title = s"\t* ${a.organization}:${a.name}$winnerRev"
        val lines = title :: (if (a.showCallers) callers.reverse else Nil) ::: List("")
        out ++= lines
    })
    out.toList
  }
}

final class EvictionError private[sbt] (
  val incompatibleEvictions: Seq[(EvictionPair, String)],
) {
  def run: Unit =
    if (incompatibleEvictions.nonEmpty) {
      sys.error(EvictionError.toLines(this).mkString("\n"))
    }
}
