package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.UnitFormatter

import scalafx.application.Platform
import scalafx.scene.control.{Button, ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml


trait CountStatsPanelController {
  def init(path: VDirectory, parentDialog: Dialog[ButtonType], showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit
  def updateStats(stats: DirStats, message: Option[String])
  def updateStats(foldersCount: Int, filesCount: Int, totalSize: Long, depth: Int, message: Option[String])
  def updateMsg(message: String)
  def showButtons(showClose: Boolean, showCancel: Boolean, showSkip: Boolean)
  def updateButtons(enableClose: Boolean, enableCancel: Boolean, enableSkip: Boolean)
}

@sfxml
class CountStatsPanelControllerImpl(headerImageView: ImageView,
                                    dirLabel: Label,
                                    foldersCountLabel: Label,
                                    filesCountLabel: Label,
                                    totalSizeLabel: Label,
                                    messageLabel: Label,

                                    closeButton: Button,
                                    cancelButton: Button,
                                    skipButton: Button,

                                    resourceMgr: ResourceManager)
    extends CountStatsPanelController {
  headerImageView.image = resourceMgr.getIcon("counter-48.png")

  override def init(path: VDirectory, parentDialog: Dialog[ButtonType],
                    showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit = {
    dirLabel.text = path.absolutePath

    closeButton.visible = showClose
    closeButton.onAction= e => {
      parentDialog.close()
      parentDialog.result = ButtonType.Close
    }

    cancelButton.visible = showCancel
    cancelButton.onAction = e => {
//      parentDialog.dialogPane().getButtonTypes.addAll(ButtonType.Cancel)
      parentDialog.close()
      parentDialog.result = ButtonType.Cancel
    }

    skipButton.visible = showSkip
    skipButton.onAction = e => {
      parentDialog.close()
      parentDialog.result = ButtonType.OK
    }
  }

  override def updateStats(stats: DirStats, message: Option[String]): Unit =
    updateStats(stats.numDirs, stats.numFiles, stats.size, stats.depth, message)

  override def updateStats(foldersCount: Int, filesCount: Int, totalSize: Long, depth: Int, message: Option[String]): Unit =
    Platform.runLater {
      foldersCountLabel.text = s"$foldersCount"
      filesCountLabel.text = s"$filesCount"
      val (size, sizeUnit) = UnitFormatter.byteUnit(totalSize)
      totalSizeLabel.text = f"$size%.2f$sizeUnit"

      message.foreach(msg => messageLabel.text = msg)
    }

  override def updateMsg(message: String): Unit =
    Platform.runLater {
      messageLabel.text = message
    }

  override def updateButtons(enableClose: Boolean, enableCancel: Boolean, enableSkip: Boolean): Unit = {
    closeButton.disable = !enableClose
    cancelButton.disable = !enableCancel
    skipButton.disable = !enableSkip
  }

  override def showButtons(showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit = {
    closeButton.visible = showClose
    cancelButton.visible = showCancel
    skipButton.visible = showSkip
  }
}
