package org.mikesajak.commander.task

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.util.UnitFormatter

case class DirStats(numFiles: Int, numDirs: Int, size: Long, depth: Int) {
  def +(other: DirStats): DirStats =
    DirStats(numFiles + other.numFiles, numDirs + other.numDirs, size + other.size, math.max(depth, other.depth))

  override def toString: String =
    s"DirCounts(numFiles=$numFiles, numDirs=$numDirs, size=${UnitFormatter.formatUnit(size)}, depth=$depth)}"
}

class DirStatsTask(rootDir: VDirectory) extends CancellableTask[DirStats] {

  override def run(progressMonitor: ProgressMonitor[DirStats]): DirStats = {
    val total = countStats(rootDir, progressMonitor, DirStats(0, 0, 0, 0), 0)
    progressMonitor.notifyFinished("", Some(total))
    total
  }

  private def dirCounts(dir: VDirectory, level: Int) = {
    val subDirs = dir.childDirs
    val files = dir.childFiles
    val filesSize = files.map(_.size).sum
    val dirsSize = subDirs.map(_.size).sum

    DirStats(files.size, subDirs.size, filesSize + dirsSize, level)
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
