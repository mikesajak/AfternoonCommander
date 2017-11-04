package org.mikesajak.commander.ui.controller.ops

import scalafx.scene.control.{Label, ProgressBar}
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml

trait MultiProgressPanelController {
  def init(operationName: String, operationDetails: String, operationIcon: Image): Unit

  def updateIndeterminate(details: String)
  def update(details: String, curProgress: Double, totalProgress: Double)
}

@sfxml
class MultiMultiProgressPanelControllerImpl(headerImageView: ImageView,
                                            nameLabel: Label,
                                            detailsLabel: Label,
                                            curProgressBar: ProgressBar,
                                            totalProgressBar: ProgressBar)
    extends MultiProgressPanelController {

  override def init(operationName: String, operationDetails: String, operationIcon: Image): Unit = {
    headerImageView.image = operationIcon
    nameLabel.text= operationName
    detailsLabel.text = operationDetails
  }

  override def updateIndeterminate(details: String): Unit = {
    detailsLabel.text = details
    curProgressBar.progress = -1
    totalProgressBar.progress = -1
  }

  override def update(details: String, curProgress: Double, totalProgress: Double): Unit = {
    detailsLabel.text = details
    curProgressBar.progress = curProgress
    totalProgressBar.progress = totalProgress
  }
}
