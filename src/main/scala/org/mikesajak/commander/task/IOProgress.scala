package org.mikesajak.commander.task

case class IOProgress(transferState: Option[TransferState], summary: IOTaskSummary,
                      curMessage: Option[String], jobStats: Option[DirStats])

object IOProgress {
  def apply(transferState: TransferState, summary: IOTaskSummary,
            curMessage: Option[String], jobStats: Option[DirStats]): IOProgress =
    IOProgress(Some(transferState), summary, curMessage, jobStats)

  def calcProgress(summary: IOTaskSummary, stats: DirStats): Double =
    calcProgress(summary).toDouble / getProgressMax(stats)

  def calcProgress(summary: IOTaskSummary): Int = summary.numDirs + summary.numFiles

  def getProgressMax(stats: DirStats): Int = stats.numDirs + stats.numFiles
}
