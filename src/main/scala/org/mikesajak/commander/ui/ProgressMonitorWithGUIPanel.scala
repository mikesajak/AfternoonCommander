package org.mikesajak.commander.ui

import org.mikesajak.commander.task.{IOTaskSummary, ProgressMonitor}
import org.mikesajak.commander.ui.controller.ops.ProgressPanelController
import scalafx.application.Platform

class ProgressMonitorWithGUIPanel(progressPanelController: ProgressPanelController) extends ProgressMonitor[IOTaskSummary] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.updateIndeterminate(message.getOrElse(""), state)
    }

  override def notifyProgress(progress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.update(message.getOrElse(""), progress, state)
    }

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.detailedUpdate(message.getOrElse(""), partProgress, totalProgress, state)
    }

  override def notifyFinished(message: Option[String], state: Option[IOTaskSummary]): Unit =
    Platform.runLater {
      progressPanelController.updateFinished(message.getOrElse(""), state)
    }

  override def notifyError(message: String, state: Option[IOTaskSummary]): Unit = ???
  // TODO: ask if continue or abort?

  override def notifyAborted(message: Option[String]): Unit =
    Platform.runLater {
      progressPanelController.updateAborted(message)
    }
}