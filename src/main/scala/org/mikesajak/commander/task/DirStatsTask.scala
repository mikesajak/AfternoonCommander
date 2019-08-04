package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}

class DirStatsTask(paths: Seq[VPath]) extends jfxc.Task[DirStats] {
  private val logger = Logger("DirStatsTask")

  updateTitle(s"DirStatsTask($paths)")

  override def call(): DirStats = try {
    updateProgress(-1, -1) // set progress indeterminate

    val total = paths.map(path => countStats(path))
                    .reduceLeft((acc, stats) => acc + stats)

    updateProgress(1, 1)
    total
  } catch {
    case _: CancelledException[Nothing] =>
      logger.info(s"Task $this has been cancelled.")
      updateMessage(s"Operation has been cancelled.") // TODO: i18
      null
    case e: Exception =>
      updateMessage(e.getLocalizedMessage)
      throw e
  }

  private def countStats(path: VPath): DirStats = {
    path match {
      case d: VDirectory => countStats(d, DirStats(0, 1, 0, 1), 0)
      case f: VFile => DirStats.ofFile(f)
    }
  }

  private def countStats(dir: VDirectory, globalCounts: DirStats, level: Int): DirStats = {
    if (isCancelled)
      throw new CancelledException

    val curCounts = globalCounts + DirStats.ofDir(dir, level)
    val totalCounts = (dir.childDirs foldLeft curCounts) ((accCounts, subDir) =>
                                                            countStats(subDir, accCounts, level + 1))

    updateValue(totalCounts)
    totalCounts
  }
}
