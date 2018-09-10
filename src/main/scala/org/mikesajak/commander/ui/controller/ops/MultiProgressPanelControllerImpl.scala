package org.mikesajak.commander.ui.controller.ops

import javafx.scene.control
import org.mikesajak.commander.task.CancellableTask
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml

@sfxml
class MultiProgressPanelControllerImpl(headerImageView: ImageView,
                                       nameLabel: Label,
                                       detailsLabel: Label,
                                       curProgressBar: ProgressBar,
                                       totalProgressBar: ProgressBar,
                                       dontCloseCheckbox: CheckBox)
    extends ProgressPanelController {

  private var dialog: Dialog[ButtonType] = _

  override def init(operationName: String, headerText: String, details1: String, details2: String,
                    operationIcon: Image, dialog: Dialog[ButtonType], task: CancellableTask): Unit = {
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

  override def updateIndeterminate(details: String): Unit = {
    detailsLabel.text = details
    curProgressBar.progress = -1
    totalProgressBar.progress = -1
  }

  override def update(details: String, progress: Float): Unit = {
    detailsLabel.text = details
    totalProgressBar.progress = progress
    curProgressBar.progress = 0
  }

  override def update(details: String, curProgress: Float, totalProgress: Float): Unit = {
    detailsLabel.text = details
    curProgressBar.progress = curProgress
    totalProgressBar.progress = totalProgress
  }

  override def updateFinished(details: String): Unit = {
    detailsLabel.text = details
    totalProgressBar.progress = 1
    curProgressBar.progress = 1
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Close)
    if (!dontCloseCheckbox.selected.value) {
      dialog.result = ButtonType.Close
    }
  }

  override def updateAborted(details: String): Unit = {
    detailsLabel.text = details
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Close)
    if (!dontCloseCheckbox.selected.value) {
      dialog.result = ButtonType.Close
    }
  }
}
