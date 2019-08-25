package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.{PathToParent, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.controller.ops.CountStatsPanelController
import scalafx.Includes._

import scala.concurrent.Future

trait OperationController[A] {
  def finalStatus: Future[Option[A]]
  def requestAbort(): Unit
}

class CountDirStatsOperationCtrl(statusMgr: StatusMgr,
                                 appController: ApplicationController,
                                 resourceMgr: ResourceManager) {
  private val logger = Logger[CountDirStatsOperationCtrl]

  def handleCountDirStats(): Unit = {
    val selectedPath = statusMgr.selectedTabManager.selectedTab.controller.focusedPath
    selectedPath match {
      case sp if !sp.isDirectory => logger.debug(s"Cannot run count stats on file: $selectedPath")
      case sp if sp.isInstanceOf[PathToParent] => logger.debug(s"Cannot run count stats on PathToParent: $sp")
      case _ => runCountDirStats(List(selectedPath.directory), autoClose = false)
    }
  }

  def runCountDirStats(selectedDirs: Seq[VPath], autoClose: Boolean): Option[DirStats] = {
    val contentLayout = "/layout/ops/count-stats-dialog.fxml"
    val (contentPane, contentCtrl) = UILoader.loadScene[CountStatsPanelController](contentLayout)

    val dialog = UIUtils.mkModalDialog[DirStats](appController.mainStage, contentPane)
    dialog.title = resourceMgr.getMessage("app.name")

    contentCtrl.init(selectedDirs, dialog, autoClose)

    dialog.showAndWait().asInstanceOf[Option[DirStats]]
  }
}
