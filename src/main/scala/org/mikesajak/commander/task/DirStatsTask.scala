package org.mikesajak.commander.task

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.util.UnitFormatter

case class DirStats(dir: VDirectory, numFiles: Int, numDirs: Int, totalNumFiles: Int, totalNumDirs: Int,
                    curDirSize: Long, totalSubTreeSize: Long, subTreeLevels: Int) {
  override def toString: String = {
    val (dirSize, dirSizeUnit) = UnitFormatter.byteUnit(curDirSize)
    val (totalSize, totalSizeUnit) = UnitFormatter.byteUnit(totalSubTreeSize)
    f"DirStats(${dir.name}, numFiles=$numFiles, numDirs=$numDirs, totalNumFiles=$totalNumFiles, totalNumDirs=$totalNumDirs, " +
      f"curDirSize=$dirSize%.2f$dirSizeUnit, totalSubTreeSize=$totalSize%.2f$totalSizeUnit), subTreeLevels=$subTreeLevels"
  }
}

class DirStatsTask(rootDir: VDirectory) extends Task{
  override def run(progressMonitor: ProgressMonitor): Unit = {
    countStats(rootDir, progressMonitor, 0)
  }

  private def countStats(dir: VDirectory, progressMonitor: ProgressMonitor, level: Int): DirStats = {
    def indent(l: Int) = "  " * l

    val subDirs = dir.childDirs
    val files = dir.childFiles

    val filesSize = files.map(f => f.size).sum

    progressMonitor.updateIndeterminate(s"${indent(level)}Counting stats for dir ${dir.name}: files=${files.size}, dirs=${subDirs.size}, files size=$filesSize")

    val subStats = subDirs.map(d => countStats(d, progressMonitor, level + 1))
    val subTreeSize = subStats.map(s => s.totalSubTreeSize).sum

    val numFiles = files.size
    val numDirs = subDirs.size

    val subNumFiles = subStats.map(s => s.totalNumFiles).sum
    val subNumDirs = subStats.map(s => s.totalNumDirs).sum

    val subLevels = (subStats foldLeft level)((maxLevel, stats) => math.max(maxLevel, stats.subTreeLevels)) + 1

    val stats = DirStats(dir, numFiles, numDirs, numFiles + subNumFiles, numDirs + subNumDirs, filesSize, filesSize + subTreeSize, subLevels)
    progressMonitor.updateIndeterminate(s"$stats")
    stats
  }
}