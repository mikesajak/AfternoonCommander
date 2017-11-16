package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{PathToParent, VDirectory}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.{DirStats, DirStatsTask}
import org.mikesajak.commander.ui.controller.ops.CountStatsPanelController
import org.mikesajak.commander.{ApplicationController, TaskManager}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.ButtonType

class CountDirStatsOperationCtrl(statusMgr: StatusMgr, taskManager: TaskManager, appController: ApplicationController) {
  private val logger = Logger[CountDirStatsOperationCtrl]

  def handleCountDirStats(): Unit = {
    val selectedPath = statusMgr.selectedTabManager.selectedTab.controller.selectedPath
    selectedPath match {
      case sp if !sp.isDirectory => logger.debug(s"Cannot run count stats on file: $selectedPath")
      case sp if sp.isInstanceOf[PathToParent] => logger.debug(s"Cannot run count stats on PathToParent: $sp")
      case _ => runCountDirStats(selectedPath.directory, autoClose = false)
    }
  }

  def runCountDirStats(selectedDir: VDirectory, autoClose: Boolean): Try[Option[DirStats]] = {
    val contentLayout = "/layout/ops/count-stats-dialog.fxml"
    val (contentPane, contentCtrl) = UILoader.loadScene[CountStatsPanelController](contentLayout)

    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
    dialog.title = "Afternoon commander"

    contentCtrl.init(selectedDir, dialog, showClose = true, showCancel = true, showSkip = false)
    contentCtrl.updateButtons(enableClose = false, enableCancel = true, enableSkip = false)

    val progressMonitor = new CountStatsProgressMonitor(contentCtrl)
    val dirStatsResult = taskManager.runTaskAsync(new DirStatsTask(selectedDir), progressMonitor)

    if (autoClose)
      dirStatsResult.foreach(stats => Platform.runLater { dialog.result = ButtonType.OK })

    val dialogResult = dialog.showAndWait()
    dialogResult match {
      case Some(ButtonType.OK) => dirStatsResult.value.get // wtf: this is ugly!
      case Some(ButtonType.Yes) => dirStatsResult.value.get
      case Some(ButtonType.Next) => dirStatsResult.value.get
      case _ => Success(None)
    }
  }
}
