package org.mikesajak.commander.ui.controller.ops

import javafx.scene.control

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml


trait ProgressPanelController {
  def init(operationName: String, headerText: String, details1: String, details2: String,
           operationIcon: Image, dialog: Dialog[ButtonType])

  def updateIndeterminate(details: String)
  def update(details: String, progress: Double)
}

@sfxml
class ProgressPanelControllerImpl(nameLabel: Label,
                                  detailsLabel: Label,
                                  progressBar: ProgressBar)
    extends ProgressPanelController {

  override def init(operationName: String, headerText: String, details1: String, details2: String,
                    operationIcon: Image, dialog: Dialog[ButtonType]): Unit = {
//    headerImageView.image = operationIcon

    dialog.title = s"Afternoon Commander - $operationName"
    dialog.headerText = headerText
    dialog.graphic = new ImageView(operationIcon)

    nameLabel.text= details1
    detailsLabel.text = details2

    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Cancel)

    val cancelButton = new Button(dialog.getDialogPane.lookupButton(ButtonType.Cancel).asInstanceOf[control.Button])
    cancelButton.filterEvent(ActionEvent.Any) {
      (ae: ActionEvent) =>
        println(s"Suppressing cancel: $ae")
    }
  }

  override def updateIndeterminate(details: String): Unit = {
    detailsLabel.text = details
    progressBar.progress = -1
  }

  override def update(details: String, progress: Double): Unit = {
    detailsLabel.text = details
    progressBar.progress = progress
  }
}
