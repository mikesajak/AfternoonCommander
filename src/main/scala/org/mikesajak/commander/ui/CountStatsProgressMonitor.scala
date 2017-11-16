package org.mikesajak.commander.ui

import org.mikesajak.commander.task.{DirStats, ProgressMonitor}
import org.mikesajak.commander.ui.controller.ops.CountStatsPanelController

class CountStatsProgressMonitor(contentCtrl: CountStatsPanelController) extends ProgressMonitor[DirStats] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[DirStats]): Unit = {
    state.foreach(s => contentCtrl.updateStats(s, message))
  }

  override def notifyProgress(progress: Float, message: Option[String], state: Option[DirStats]): Unit = {
    state.foreach(s => contentCtrl.updateStats(s, message))
  }

  override def notifyFinished(message: String, state: Option[DirStats]): Unit = {
    println(s"Finished: $message, stats=$state")
    state.foreach(s => contentCtrl.updateStats(s, Some(message)))
    //        contentCtrl.showButtons(true, )
    contentCtrl.updateButtons(enableClose = true, enableCancel = false, enableSkip = false)
  }

  override def notifyError(message: String, state: Option[DirStats]): Unit = {
    state match {
      case Some(stats) => contentCtrl.updateStats(stats, Some(message))
      case _ => contentCtrl.updateMsg(message)
    }
    //        contentCtrl.showButtons(showClose = true, showCancel = false, showSkip = false)
    contentCtrl.updateButtons(enableClose = true, enableCancel = false, enableSkip = false)
  }

  override def notifyAborted(message: String): Unit = {
    // TODO: do nothing?
  }
}
