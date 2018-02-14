package org.mikesajak.commander.ui

import org.mikesajak.commander.task.{IOTaskSummary, ProgressMonitor}
import org.mikesajak.commander.ui.controller.ops.ProgressPanelController

import scalafx.application.Platform

class ProgressMonitorWithGUIPanel(progressPanelController: ProgressPanelController) extends ProgressMonitor[IOTaskSummary] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.updateIndeterminate(message.getOrElse(""))
    }

  override def notifyProgress(progress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.update(message.getOrElse(""), progress)
    }

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.update(message.getOrElse(""), partProgress, totalProgress)
    }

  override def notifyFinished(message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.updateFinished(message.getOrElse(""))
    }

  override def notifyError(message: String, state: Option[IOTaskSummary]): Unit = ???
  // TODO: ask if continue or abort?

  override def notifyAborted(message: String): Unit =
    Platform.runLater {
      progressPanelController.updateAborted(message)
    }
}