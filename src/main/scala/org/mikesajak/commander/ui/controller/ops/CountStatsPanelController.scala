package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.Utils.MyRichBoolean

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.{ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

trait CountStatsPanelController {
  def init(path: VDirectory, parentDialog: Dialog[ButtonType], showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit
  def updateStats(stats: DirStats, message: Option[String])
  def updateMsg(message: String)
  def showButtons(showClose: Boolean, showCancel: Boolean, showSkip: Boolean)
  def updateButtons(enableClose: Boolean, enableCancel: Boolean, enableSkip: Boolean)
}

@sfxml
class CountStatsPanelControllerImpl(headerImageView: ImageView,
                                    dirLabel: Label,
                                    dirStatsPanel: Pane,
                                    @nested[DirStatsPanelControllerImpl] dirStatsPanelController: DirStatsPanelController,

                                    messageLabel: Label,

                                    resourceMgr: ResourceManager)
    extends CountStatsPanelController {
  private var dialog: Dialog[ButtonType] = _

  headerImageView.image = resourceMgr.getIcon("counter-48.png")

  override def init(path: VDirectory, parentDialog: Dialog[ButtonType],
                    showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit = {
    this.dialog = parentDialog
    dirLabel.text = path.absolutePath
    dirLabel.graphic = new ImageView(resourceMgr.getIcon("folder-24.png"))

    dirStatsPanelController.init(path, None)

    dialog.dialogPane().buttonTypes =
      List(showClose.option(ButtonType.Close),
           showCancel.option(ButtonType.Cancel),
           showSkip.option(ButtonType.Next)).flatten
  }

  override def updateStats(stats: DirStats, message: Option[String]): Unit =
    Platform.runLater {
      dirStatsPanelController.updateStats(stats)
      message.foreach(msg => messageLabel.text = msg)
    }

  override def updateMsg(message: String): Unit =
    Platform.runLater {
      messageLabel.text = message
    }

  override def updateButtons(enableClose: Boolean, enableCancel: Boolean, enableSkip: Boolean): Unit = {
    Option(dialog.dialogPane().lookupButton(ButtonType.Close)).foreach(_.disable = !enableClose)
    Option(dialog.dialogPane().lookupButton(ButtonType.Cancel)).foreach(_.disable = !enableCancel)
    Option(dialog.dialogPane().lookupButton(ButtonType.Next)).foreach(_.disable = !enableSkip)
  }

  override def showButtons(showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit = {
    Option(dialog.dialogPane().lookupButton(ButtonType.Close)).foreach(_.visible = !showClose)
    Option(dialog.dialogPane().lookupButton(ButtonType.Cancel)).foreach(_.visible = !showCancel)
    Option(dialog.dialogPane().lookupButton(ButtonType.Next)).foreach(_.visible = !showSkip)
  }
}
