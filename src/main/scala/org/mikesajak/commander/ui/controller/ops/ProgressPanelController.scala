package org.mikesajak.commander.ui.controller.ops

import scalafx.scene.control.{Label, ProgressBar}
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml


trait ProgressPanelController {
  def init(operationName: String, operationDetails: String, operationIcon: Image): Unit

  def updateIndeterminate(details: String)
  def update(details: String, progress: Double)
}

@sfxml
class ProgressPanelControllerImpl(headerImageView: ImageView,
                                  nameLabel: Label,
                                  detailsLabel: Label,
                                  progressBar: ProgressBar)
    extends ProgressPanelController {

  override def init(operationName: String, operationDetails: String, operationIcon: Image): Unit = {
    headerImageView.image = operationIcon
    nameLabel.text= operationName
    detailsLabel.text = operationDetails
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
