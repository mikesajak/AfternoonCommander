package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.task.{DirCounts, DirStats}
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.UnitFormatter

import scalafx.application.Platform
import scalafx.scene.control.{Button, ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml


trait CountStatsPanelController {
  def init(path: VDirectory, parentDialog: Dialog[DirStats]): Unit
  def updateStats(stats: DirCounts, message: Option[String])
  def updateStats(foldersCount: Int, filesCount: Int, totalSize: Long, depth: Int, message: Option[String])
  def updateMsg(message: String)
}

@sfxml
class CountStatsPanelControllerImpl(headerImageView: ImageView,
                                    dirLabel: Label,
                                    foldersCountLabel: Label,
                                    filesCountLabel: Label,
                                    totalSizeLabel: Label,
                                    messageLabel: Label,
                                    cancelButton: Button,
                                    skipButton: Button,

                                    resourceMgr: ResourceManager)
    extends CountStatsPanelController {
  headerImageView.image = resourceMgr.getIcon("counter-48.png")

  override def init(path: VDirectory, parentDialog: Dialog[DirStats]): Unit = {
    dirLabel.text = path.absolutePath

    cancelButton.onAction = e => {
      parentDialog.dialogPane().getButtonTypes.addAll(ButtonType.Cancel)
      parentDialog.close()
      parentDialog.result = null
    }
  }

  override def updateStats(stats: DirCounts, message: Option[String]): Unit =
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
}
