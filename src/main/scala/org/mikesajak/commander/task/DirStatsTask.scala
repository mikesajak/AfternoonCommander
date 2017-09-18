package org.mikesajak.commander.task

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.util.UnitFormatter

object DirStats {
  def apply(dir: VDirectory, numFiles: Int, numDirs: Int, curDirSize: Long) =
    new DirStats(dir, numFiles, numDirs, numFiles, numDirs, curDirSize, curDirSize, 0)
}

case class DirStats(dir: VDirectory, numFiles: Int, numDirs: Int, totalNumFiles: Int, totalNumDirs: Int,
                    curDirSize: Long, totalSubTreeSize: Long, subTreeLevels: Int) {

  def mergeChildStats(stats2: DirStats): DirStats = {
    DirStats(dir, numFiles, numDirs, numFiles + stats2.totalNumFiles, numDirs + stats2.totalNumDirs,
             curDirSize, curDirSize + stats2.totalSubTreeSize, math.max(subTreeLevels, stats2.subTreeLevels))
  }

  override def toString: String = {
    val (dirSize, dirSizeUnit) = UnitFormatter.byteUnit(curDirSize)
    val (totalSize, totalSizeUnit) = UnitFormatter.byteUnit(totalSubTreeSize)
    f"DirStats(${dir.name}, numFiles=$numFiles, numDirs=$numDirs, totalNumFiles=$totalNumFiles, totalNumDirs=$totalNumDirs, " +
      f"curDirSize=$dirSize%.2f$dirSizeUnit, totalSubTreeSize=$totalSize%.2f$totalSizeUnit), subTreeLevels=$subTreeLevels"
  }
}

class DirStatsTask(rootDir: VDirectory) extends Task[DirStats] {
  override def run(progressMonitor: ProgressMonitor2[DirStats]): DirStats = {
    countStats2(rootDir, progressMonitor, 0, DirStats(rootDir, 0, 0, 0, 0, 0, 0, 0))
  }

  private def countStats(dir: VDirectory, progressMonitor: ProgressMonitor2[DirStats], level: Int): DirStats = {
    def indent(l: Int) = "  " * l

    val subDirs = dir.childDirs
    val files = dir.childFiles

    val filesSize = files.map(f => f.size).sum

    val subStats = subDirs.map(d => countStats(d, progressMonitor, level + 1))

    def prepStats(): DirStats = {
      val subTreeSize = subStats.map(s => s.totalSubTreeSize).sum

      val numFiles = files.size
      val numDirs = subDirs.size

      val subNumFiles = subStats.map(s => s.totalNumFiles).sum
      val subNumDirs = subStats.map(s => s.totalNumDirs).sum

      val subLevels = (subStats foldLeft level)((maxLevel, stats) => math.max(maxLevel, stats.subTreeLevels)) + 1

      DirStats(dir, numFiles, numDirs, numFiles + subNumFiles, numDirs + subNumDirs, filesSize, filesSize + subTreeSize, subLevels)
    }

    val statsOld = prepStats()
    val stats = (subStats foldLeft DirStats(dir, files.size, subDirs.size, 0, 0, filesSize, 0, level))((acc, stats) => acc.mergeChildStats(stats))

//    progressMonitor.notifyProgressIndeterminate(Some(s"${indent(level)}Counting OLD stats for dir ${dir.name}"), Some(statsOld))
//    progressMonitor.notifyProgressIndeterminate(Some(s"${indent(level)}Counting NEW stats for dir ${dir.name}"), Some(stats))
    progressMonitor.notifyProgressIndeterminate(None, Some(stats))
    stats
  }

  private def countStats2(dir: VDirectory, progressMonitor: ProgressMonitor2[DirStats], level: Int, totalStats: DirStats): DirStats = {
    def indent(l: Int) = "  " * l

    val subDirs = dir.childDirs
    val files = dir.childFiles

    val filesSize = files.map(f => f.size).sum

    val curStats = DirStats(dir, files.size, subDirs.size, filesSize)

//    val subStats = subDirs.map(d => countStats2(d, progressMonitor, level + 1, ))

//    val stats = (subDirs foldLeft totalStats)((stats, curDir) => stats.mergeChildStats(countStats2(curDir, progressMonitor, level + 1, stats)))
//    var stats = totalStats.mergeChildStats(curStats)
    var stats = curStats.mergeChildStats(totalStats)
    for (curDir <- subDirs) {
      val subStats = countStats2(curDir, progressMonitor, level + 1, stats)
      println(s"${indent(level)}subStats=$subStats")
      stats = stats.mergeChildStats(subStats)
      println(s"${indent(level)} upStats=$stats")
    }

//    val stats = (subStats foldLeft totalStats)((acc, stats) => acc.mergeChildStats(stats))

    //    progressMonitor.notifyProgressIndeterminate(Some(s"${indent(level)}Counting OLD stats for dir ${dir.name}"), Some(statsOld))
    //    progressMonitor.notifyProgressIndeterminate(Some(s"${indent(level)}Counting NEW stats for dir ${dir.name}"), Some(stats))
    progressMonitor.notifyProgressIndeterminate(Some(s"Current folder: ${stats.dir.absolutePath}"), Some(stats))
    stats
  }
}
