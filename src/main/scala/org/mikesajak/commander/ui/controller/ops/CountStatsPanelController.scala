package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.{IconSize, ResourceManager, StatsUpdateListener}
import org.mikesajak.commander.util.Utils.MyRichBoolean
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.{ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

trait CountStatsPanelController extends StatsUpdateListener {
  def init(path: Seq[VPath], parentDialog: Dialog[ButtonType], showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit
  def showButtons(showClose: Boolean, showCancel: Boolean, showSkip: Boolean)
  def updateButtons(enableClose: Boolean, enableCancel: Boolean, enableSkip: Boolean)

  override def updateStats(stats: DirStats, message: Option[String])
  override def updateMessage(message: String)
  override def notifyFinished(stats: DirStats, message: Option[String])
  override def notifyError(stats: Option[DirStats], message: String)
}

@sfxml
class CountStatsPanelControllerImpl(headerImageView: ImageView,
                                    dirLabel: Label,
                                    statsPanel: Pane,
                                    @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,
                                    messageLabel: Label,

                                    resourceMgr: ResourceManager)
    extends CountStatsPanelController {
  private var dialog: Dialog[ButtonType] = _

  headerImageView.image = resourceMgr.getIcon("counter.png", IconSize.Big)

  override def init(paths: Seq[VPath], parentDialog: Dialog[ButtonType],
                    showClose: Boolean, showCancel: Boolean, showSkip: Boolean): Unit = {
    this.dialog = parentDialog
    dirLabel.text = if (paths.size == 1) paths.head.absolutePath
                    else s"${paths.size} paths"
    dirLabel.graphic = new ImageView(resourceMgr.getIcon("folder.png", IconSize.Small))

    statsPanel.height.onChange { (_, oldVal, newVal) =>
      if (newVal.doubleValue > oldVal.doubleValue) dialog.dialogPane.value.getScene.getWindow.sizeToScene()
    }
    statsPanel.width.onChange { (_, oldVal, newVal) =>
      if (newVal.doubleValue > oldVal.doubleValue) dialog.dialogPane.value.getScene.getWindow.sizeToScene()
    }
    statsPanelController.init(paths)

    messageLabel.graphic = new ImageView(resourceMgr.getIcon2("loading-chasing-arrows.gif"))
    messageLabel.text = resourceMgr.getMessage("count_stats_dialog.counting.label")

    dialog.dialogPane().buttonTypes =
      List(showClose.option(ButtonType.Close),
           showCancel.option(ButtonType.Cancel),
           showSkip.option(ButtonType.Next)).flatten
  }

  override def updateStats(stats: DirStats, message: Option[String]): Unit = {
    Platform.runLater {
      statsPanelController.updateStats(stats)
      message.foreach(msg => messageLabel.text = msg)
    }
  }

  override def updateMessage(message: String): Unit =
    Platform.runLater {
      messageLabel.text = message
    }

  override def notifyFinished(stats: DirStats, message: Option[String]): Unit = {
    updateStats(stats, message)
    Platform.runLater {
      messageLabel.graphic = null
      messageLabel.text = null
    }
    updateButtons(enableClose = true, enableCancel = false, enableSkip = false)
  }

  override def notifyError(stats: Option[DirStats], message: String): Unit = {
    stats match {
      case Some(s) => updateStats(s, Some(message))
      case _ => updateMessage(message)
    }
    Platform.runLater {
      messageLabel.graphic = null
    }
    updateButtons(enableClose = true, enableCancel = false, enableSkip = false)
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
