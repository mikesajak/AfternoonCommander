package org.mikesajak.commander.ui.controller.ops

import javafx.scene.{control => jfxctrl}
import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.{BackgroundService, DirStats, DirStatsTask}
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import scalafx.Includes._
import scalafx.concurrent.Service
import scalafx.scene.control.{ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

trait CountStatsPanelController {
  def init(path: Seq[VPath], parentDialog: Dialog[DirStats], autoClose: Boolean): Service[DirStats]
}

@sfxml
class CountStatsPanelControllerImpl(headerImageView: ImageView,
                                    dirLabel: Label,
                                    statsPanel: Pane,
                                    @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,

                                    resourceMgr: ResourceManager)
    extends CountStatsPanelController {
  private var dialog: Dialog[DirStats] = _
  private var autoClose: Boolean = false

  headerImageView.image = resourceMgr.getIcon("counter.png", IconSize.Big)

  override def init(paths: Seq[VPath], parentDialog: Dialog[DirStats], autoClose: Boolean): Service[DirStats] = {
    this.dialog = parentDialog
    this.autoClose = autoClose

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

    val statsService = new BackgroundService(new DirStatsTask(paths))
    statsService.onRunning = e => statsPanelController.notifyStarted()
    statsService.onFailed = e => notifyError(Option(statsService.value.value), statsService.message.value)
    statsService.onSucceeded = e => notifyFinished(statsService.value.value)
    statsService.value.onChange { (_, _, stats) => statsPanelController.updateStats(stats, None)}

    dialog.onShown = e => statsService.start()

    dialog.dialogPane().buttonTypes = List(ButtonType.Close, ButtonType.Cancel)

    parentDialog.resultConverter = {
      case ButtonType.OK => statsService.getValue
      case ButtonType.Close => statsService.getValue
      case ButtonType.Cancel => null
    }

    Option(dialog.dialogPane().lookupButton(ButtonType.Cancel))
        .map(_.asInstanceOf[jfxctrl.Button])
        .foreach(b => b.onAction = ae => statsService.cancel())

    updateButtons(false, true, false)
    statsService
  }

  private def notifyFinished(stats: DirStats, message: Option[String] = None): Unit = {
    statsPanelController.notifyFinished(stats, message)
    updateButtons(enableClose = true, enableCancel = false, enableSkip = false)

    if (autoClose)
      Option(dialog.dialogPane().lookupButton(ButtonType.Close))
        .map(_.asInstanceOf[jfxctrl.Button])
        .foreach { b => if (autoClose) b.fire() }
  }

  private def notifyError(stats: Option[DirStats], message: String): Unit = {
    statsPanelController.notifyError(stats, message)
    updateButtons(enableClose = true, enableCancel = false, enableSkip = false)
  }

  private def updateButtons(enableClose: Boolean, enableCancel: Boolean, enableSkip: Boolean): Unit = {
    Option(dialog.dialogPane().lookupButton(ButtonType.Close)).foreach(_.disable = !enableClose)
    Option(dialog.dialogPane().lookupButton(ButtonType.Cancel)).foreach(_.disable = !enableCancel)
    Option(dialog.dialogPane().lookupButton(ButtonType.Next)).foreach(_.disable = !enableSkip)
  }
}
