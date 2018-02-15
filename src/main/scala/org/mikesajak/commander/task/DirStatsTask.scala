package org.mikesajak.commander.task

import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.task.CancellableTask._
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

class DirStatsTask(paths: Seq[VPath]) extends Task[DirStats] with CancellableTask {

  override def run(progressMonitor: ProgressMonitor[DirStats]): Option[DirStats] = {
    withAbort(progressMonitor) { () =>
      val total = paths.map(path => countStats(path, progressMonitor))
                       .reduceLeft((acc, stats) => acc + stats)

      progressMonitor.notifyFinished(None, Some(total))
      total
    }
  }

  private def dirCounts(dir: VDirectory, level: Int) = {
    val subDirs = dir.childDirs
    val files = dir.childFiles
    val filesSize = files.map(_.size).sum
    val dirsSize = subDirs.map(_.size).sum

    DirStats(files.size, subDirs.size, filesSize + dirsSize, level)
  }

  private def countStats(path: VPath, progressMonitor: ProgressMonitor[DirStats]): DirStats = {
    path match {
      case d: VDirectory => countStats(d, progressMonitor, DirStats.Empty, 0)
      case f: VFile => DirStats(1, 0, f.size, 0)
    }
  }

  private def countStats(dir: VDirectory, progressMonitor: ProgressMonitor[DirStats], globalCounts: DirStats, level: Int): DirStats = {
    abortIfNeeded()

    val curCounts = globalCounts + dirCounts(dir, level)
    val totalCounts = (dir.childDirs foldLeft curCounts) ((accCounts, subDir) =>
      countStats(subDir, progressMonitor, accCounts, level + 1))

    progressMonitor.notifyProgressIndeterminate(Some(s"Current folder: ${dir.absolutePath}"), Some(totalCounts))
    totalCounts
  }
}
