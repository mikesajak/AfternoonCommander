package org.mikesajak.commander.ui

import org.mikesajak.commander.task.{DirStats, ProgressMonitor}

trait StatsUpdateListener {
  def updateStats(stats: DirStats, message: Option[String])
  def updateMessage(message: String)
  def notifyFinished(stats: DirStats, message: Option[String])
  def notifyError(stats: Option[DirStats], message: String)
}

class CountStatsProgressMonitor(statsListener: StatsUpdateListener) extends ProgressMonitor[DirStats] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[DirStats]): Unit = {
    state.foreach(s => statsListener.updateStats(s, message))
  }

  override def notifyProgress(progress: Float, message: Option[String], state: Option[DirStats]): Unit = {
    state.foreach(s => statsListener.updateStats(s, message))
  }

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[DirStats]): Unit = {
    state.foreach(s => statsListener.updateStats(s, message))
  }

  override def notifyFinished(message: Option[String], state: Option[DirStats]): Unit = {
    statsListener.notifyFinished(state.get, message)
  }

  override def notifyError(message: String, state: Option[DirStats]): Unit = {
    statsListener.notifyError(state, message)
  }

  override def notifyAborted(message: String): Unit = {
    // TODO: do nothing?
  }
}
