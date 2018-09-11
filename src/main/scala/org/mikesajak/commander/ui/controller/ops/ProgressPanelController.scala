package org.mikesajak.commander.ui.controller.ops

import javafx.scene.control
import org.mikesajak.commander.task.{CancellableTask, IOTaskSummary}
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml

import scala.language.implicitConversions

trait ProgressPanelController {
  def init(operationName: String, headerText: String, details1: String, details2: String,
              operationIcon: Image, dialog: Dialog[ButtonType],
              task: CancellableTask)

  def updateIndeterminate(details: String, stats: Option[IOTaskSummary] = None): Unit
  def update(details: String, progress: Float, stats: Option[IOTaskSummary] = None): Unit
  def detailedUpdate(details: String, partProgress: Float, totalProgress: Float, stats: Option[IOTaskSummary] = None): Unit
  def updateFinished(details: String, stats: Option[IOTaskSummary] = None): Unit
  def updateAborted(details: Option[String], stats: Option[IOTaskSummary] = None): Unit
}

@sfxml
class ProgressPanelControllerImpl(nameLabel: Label,
                                  detailsLabel: Label,
                                  progressBar: ProgressBar,
                                  fileCountLabel: Label,
                                  dirCountLabel: Label,
                                  sizeLabel: Label,
                                  elapsedTimeLabel: Label,
                                  estimatedTimeLabel: Label,
                                  dontCloseCheckbox: CheckBox)
    extends ProgressPanelController {

  private var dialog: Dialog[ButtonType] = _

  override def init(operationName: String, headerText: String, details1: String, details2: String,
                       operationIcon: Image, dialog: Dialog[ButtonType],
                       task: CancellableTask): Unit = {
    this.dialog = dialog

    dialog.title = s"Afternoon Commander - $operationName"
    dialog.headerText = headerText
    dialog.graphic = new ImageView(operationIcon)

    nameLabel.text= details1
    detailsLabel.text = details2

    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Cancel)

    val cancelButton = new Button(dialog.getDialogPane.lookupButton(ButtonType.Cancel).asInstanceOf[control.Button])
    cancelButton.filterEvent(ActionEvent.Any) { ae: ActionEvent =>
      println(s"Suppressing cancel: $ae")
      ae.consume() // suppress cancel, notify task to cancel
      cancelButton.disable = true
      task.cancel()
    }
  }

  override def updateIndeterminate(details: String, stats: Option[IOTaskSummary]): Unit = {
    detailsLabel.text = details
    updateStats(stats)
    progressBar.progress = -1
  }

  override def update(details: String, progress: Float, stats: Option[IOTaskSummary]): Unit = {
    detailsLabel.text = details
    updateStats(stats)
    progressBar.progress = progress
  }

  override def detailedUpdate(details: String, partProgress: Float, totalProgress: Float, stats: Option[IOTaskSummary]): Unit = {
    detailsLabel.text = details
    updateStats(stats)
    progressBar.progress = totalProgress
    // TODO: partial progress
  }

  override def updateFinished(details: String, stats: Option[IOTaskSummary]): Unit = {
    detailsLabel.text = details
    updateStats(stats)
    progressBar.progress = 1
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Close)
    if (!dontCloseCheckbox.selected.value) {
       dialog.result = ButtonType.Close
    }
  }

  override def updateAborted(details: Option[String], stats: Option[IOTaskSummary]): Unit = {
    details.foreach(msg => detailsLabel.text = msg)
    updateStats(stats)
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Close)
    if (!dontCloseCheckbox.selected.value) {
      dialog.result = ButtonType.Close
    }
  }

  private def updateStats(stats: Option[IOTaskSummary]): Unit =
    stats.foreach { s =>
      fileCountLabel.text = s.numFiles.toString
      dirCountLabel.text = s.numDirs.toString
      sizeLabel.text = s.totalSize.toString
    }
}
