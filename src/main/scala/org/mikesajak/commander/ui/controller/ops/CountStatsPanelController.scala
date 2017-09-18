package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.UnitFormatter

import scalafx.application.Platform
import scalafx.scene.control.{Dialog, Label}
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml


trait CountStatsPanelController {
  def init(path: VDirectory, parentDialog: Dialog[DirStats]): Unit
  def updateStats(stats: DirStats, message: Option[String])
  def updateStats(foldersCount: Int, filesCount: Int, totalSize: Long, message: Option[String])
  def updateMsg(message: String)
}

@sfxml
class CountStatsPanelControllerImpl(headerImageView: ImageView,
                                    dirLabel: Label,
                                    foldersCountLabel: Label,
                                    filesCountLabel: Label,
                                    totalSizeLabel: Label,
                                    messageLabel: Label,

                                    resourceMgr: ResourceManager)
    extends CountStatsPanelController {
  headerImageView.image = resourceMgr.getIcon("counter-48.png")

  override def init(path: VDirectory, parentDialog: Dialog[DirStats]): Unit = {
    dirLabel.text = path.absolutePath
  }

  override def updateStats(stats: DirStats, message: Option[String]): Unit =
    updateStats(stats.totalNumDirs, stats.totalNumFiles, stats.totalSubTreeSize, message)

  override def updateStats(foldersCount: Int, filesCount: Int, totalSize: Long, message: Option[String]): Unit =
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
