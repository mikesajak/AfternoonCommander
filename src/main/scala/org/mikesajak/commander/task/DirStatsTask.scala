package org.mikesajak.commander.task

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.util.UnitFormatter

object DirStats {
  def apply(dir: VDirectory, numFiles: Int, numDirs: Int, curDirSize: Long) =
    new DirStats(dir, numFiles, numDirs, curDirSize, numFiles, numDirs, curDirSize, 0)
}

case class DirCounts(numFiles: Int, numDirs: Int, size: Long, depth: Int) {
  def +(other: DirCounts): DirCounts =
    DirCounts(numFiles + other.numFiles, numDirs + other.numDirs, size + other.size, math.max(depth, other.depth))

  override def toString: String =
    s"DirCounts(numFiles=$numFiles, numDirs=$numDirs, size=${UnitFormatter.formatUnit(size)}, depth=$depth)}"
}

case class DirStats(dir: VDirectory, numFiles: Int, numDirs: Int, curDirSize: Long,
                    totalNumFiles: Int, totalNumDirs: Int, totalSubTreeSize: Long, subTreeLevels: Int) {

  def mergeChildStats(stats2: DirStats): DirStats = {
    DirStats(dir, numFiles, numDirs, curDirSize,
      numFiles + stats2.totalNumFiles, numDirs + stats2.totalNumDirs, curDirSize + stats2.totalSubTreeSize,
      math.max(subTreeLevels, stats2.subTreeLevels))
  }

  override def toString: String = {
    val (dirSize, dirSizeUnit) = UnitFormatter.byteUnit(curDirSize)
    val (totalSize, totalSizeUnit) = UnitFormatter.byteUnit(totalSubTreeSize)
    f"DirStats(${dir.name}, numFiles=$numFiles, numDirs=$numDirs, totalNumFiles=$totalNumFiles, totalNumDirs=$totalNumDirs, " +
      f"curDirSize=$dirSize%.2f$dirSizeUnit, totalSubTreeSize=$totalSize%.2f$totalSizeUnit), subTreeLevels=$subTreeLevels"
  }
}

class DirStatsTask(rootDir: VDirectory) extends Task[DirStats, DirCounts] {
  override def run(progressMonitor: ProgressMonitor2[DirCounts]): DirStats = {
    val total = countStats(rootDir, progressMonitor, DirCounts(0, 0, 0, 0), 0)
    progressMonitor.notifyFinished("", Some(total))
    val cur = dirCounts(rootDir, 0)
    DirStats(rootDir, cur.numFiles, cur.numDirs, cur.size, total.numFiles, total.numDirs, total.size, total.depth)
  }


  private def dirCounts(dir: VDirectory, level: Int) = {
    val subDirs = dir.childDirs
    val files = dir.childFiles
    val filesSize = files.map(_.size).sum
    val dirsSize = subDirs.map(_.size).sum

    DirCounts(files.size, subDirs.size, filesSize + dirsSize, level)
  }

  private def countStats(dir: VDirectory, progressMonitor: ProgressMonitor2[DirCounts], globalCounts: DirCounts, level: Int): DirCounts = {
    val curCounts = globalCounts + dirCounts(dir, level)
    val totalCounts = (dir.childDirs foldLeft curCounts)((accCounts, subDir) => countStats(subDir, progressMonitor, accCounts, level + 1))

    progressMonitor.notifyProgressIndeterminate(Some(s"Current folder: ${dir.absolutePath}"), Some(totalCounts))
    totalCounts
  }
}
