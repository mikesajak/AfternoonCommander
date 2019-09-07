package org.mikesajak.commander.task

import org.mikesajak.commander.fs.{VDirectory, VFile}
import org.mikesajak.commander.util.Utils
import org.mikesajak.commander.{FileType, FileTypeManager}

case class DirContents(typesMap: Map[FileType, Int], extensionsMap: Map[String, Int]) {
  def +(other: DirContents): DirContents = {
    val mergedTypesMap = Utils.merge(typesMap, other.typesMap) { case (_, v1, v2) => v1 + v2 }
    val mergedExtensionsMap = Utils.merge(extensionsMap, other.extensionsMap) { case (_, v1, v2) => v1 + v2}
    DirContents(mergedTypesMap, mergedExtensionsMap)
  }
}

object DirContents {
  val Empty = new DirContents(Map(), Map())
}

class DirContentsProcessor(fileTypeManager: FileTypeManager) extends PathProcessor[DirContents] {
  override def title: String = "Collect directory contents statistics"

  override def process(files: Seq[VFile], dirs: Seq[VDirectory], level: Int): DirContents = {
    val statsByType = (files ++ dirs)
        .groupBy(f => fileTypeManager.detectFileType(f))
        .map { case (tp, files) => (tp, files.size) }

    val statsByExt = files
        .groupBy(f => f.extension.getOrElse(""))
        .map { case (ext, files) => (ext, files.size) }

    DirContents(statsByType, statsByExt)
  }

  override def Empty: DirContents = DirContents.Empty

  override def merge(contents1: DirContents, contents2: DirContents): DirContents = contents1 + contents2
}

class DirStatsAndContentsProcessor(fileTypeManager: FileTypeManager) extends PathProcessor[(DirStats, DirContents)] {
  private val statsProcessor = new DirStatsProcessor
  private val contentsProcessor = new DirContentsProcessor(fileTypeManager)

  override def title: String = "Analyzing directory contents and statistics"

  override def process(files: Seq[VFile], dirs: Seq[VDirectory], level: Int): (DirStats, DirContents) = {
    (statsProcessor.process(files, dirs, level), contentsProcessor.process(files, dirs, level))
  }

  override def Empty: (DirStats, DirContents) = (DirStats.Empty, DirContents.Empty)

  override def merge(res1: (DirStats, DirContents), res2: (DirStats, DirContents)): (DirStats, DirContents) =
    (statsProcessor.merge(res1._1, res2._1), contentsProcessor.merge(res1._2, res2._2))
}
