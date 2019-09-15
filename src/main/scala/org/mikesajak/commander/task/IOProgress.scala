package org.mikesajak.commander.task

case class IOProgress(transferState: Option[TransferState], summary: IOTaskSummary,
                      curMessage: Option[String], jobStats: Option[DirStats])

object IOProgress {
  def apply(transferState: TransferState, summary: IOTaskSummary,
            curMessage: Option[String], jobStats: Option[DirStats]): IOProgress =
    IOProgress(Some(transferState), summary, curMessage, jobStats)

  def calcProgress(summary: IOTaskSummary, stats: DirStats): Double =
    calcProgressBySize(summary).toDouble / getProgressMaxBySize(stats)

  def calcProgressByNumFiles(summary: IOTaskSummary): Long = summary.numDirs + summary.numFiles
  def calcProgressBySize(summary: IOTaskSummary): Long = summary.totalSize

  def getProgressMaxByNumFiles(stats: DirStats): Long = stats.numDirs + stats.numFiles
  def getProgressMaxBySize(stats: DirStats): Long = stats.size
}
