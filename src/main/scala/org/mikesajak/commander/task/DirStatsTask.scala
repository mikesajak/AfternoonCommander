package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.util.UnitFormatter

case class DirStats(numFiles: Int, numDirs: Int, size: Long, depth: Int) {
  def +(other: DirStats): DirStats =
    DirStats(numFiles + other.numFiles, numDirs + other.numDirs, size + other.size, math.max(depth, other.depth))

  override def toString: String =
    s"DirCounts(numFiles=$numFiles, numDirs=$numDirs, size=${UnitFormatter.formatDataSize(size)}, depth=$depth)}"
}

object DirStats {
  val Empty = DirStats(0,0,0,0)
}

class DirStatsTask(paths: Seq[VPath]) extends scalafx.concurrent.Task(new jfxc.Task[DirStats] {
  private val logger = Logger("DirStatsTask")

  updateTitle(s"DirStatsTask($paths)")

  override def call(): DirStats = try {
    updateProgress(-1, -1) // set progress indeterminate

    val total = paths.map(path => countStats(path))
                    .reduceLeft((acc, stats) => acc + stats)

    updateProgress(1, 1)
    total
  } catch {
    case c: CancelledException =>
      logger.info(s"Task $this has been cancelled.")
      updateProgress(0, 0)
      null
    case e: Exception =>
      updateProgress(0, 0)
      updateMessage(e.getLocalizedMessage)
      throw e
  }

  private def dirCounts(dir: VDirectory, level: Int) = {
    val subDirs = dir.childDirs
    val files = dir.childFiles
    val filesSize = files.map(_.size).sum
    val dirsSize = subDirs.map(_.size).sum

    DirStats(files.size, subDirs.size, filesSize + dirsSize, level)
  }

  private def countStats(path: VPath): DirStats = {
    path match {
      case d: VDirectory => countStats(d, DirStats(0, 1, 0, 1), 0)
      case f: VFile => DirStats(1, 0, f.size, 0)
    }
  }

  private def countStats(dir: VDirectory, globalCounts: DirStats, level: Int): DirStats = {
    if (isCancelled)
      throw new CancelledException

    val curCounts = globalCounts + dirCounts(dir, level)
    val totalCounts = (dir.childDirs foldLeft curCounts) ((accCounts, subDir) =>
                                                            countStats(subDir, accCounts, level + 1))

    updateValue(totalCounts)
    totalCounts
  }
})

