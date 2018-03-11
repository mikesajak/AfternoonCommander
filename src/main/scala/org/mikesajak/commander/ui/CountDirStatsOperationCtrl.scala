package org.mikesajak.commander.ui

import javafx.scene.control.Button

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{PathToParent, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.{DirStats, DirStatsTask}
import org.mikesajak.commander.ui.controller.ops.CountStatsPanelController
import org.mikesajak.commander.{ApplicationController, TaskManager}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.ButtonType

trait OperationController[A] {
  //    def status: A
  def finalStatus: Future[Option[A]]
  def requestAbort(): Unit // Future
}

class CountDirStatsOperationCtrl(statusMgr: StatusMgr,
                                 taskMgr: TaskManager,
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

  def runCountDirStats(selectedPaths: Seq[VPath], statsListener: StatsUpdateListener): OperationController[DirStats] = {
    val progressMonitor = new CountStatsProgressMonitor(statsListener)
    val task = new DirStatsTask(selectedPaths)
    val dirStatsResult = taskMgr.runTaskAsync(task, progressMonitor)

    new OperationController[DirStats] {
      override def finalStatus: Future[Option[DirStats]] = dirStatsResult
      override def requestAbort(): Unit = task.cancel()
    }
  }

  def runCountDirStats(selectedDirs: Seq[VPath], autoClose: Boolean): Try[Option[DirStats]] = {
    val contentLayout = "/layout/ops/count-stats-dialog.fxml"
    val (contentPane, contentCtrl) = UILoader.loadScene[CountStatsPanelController](contentLayout)

    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
    dialog.title = resourceMgr.getMessage("app.name")

    contentCtrl.init(selectedDirs, dialog, showClose = true, showCancel = true, showSkip = false)
    contentCtrl.updateButtons(enableClose = false, enableCancel = true, enableSkip = false)

    val statsTask = new DirStatsTask(selectedDirs)

    dialog.dialogPane.value.lookupButton(ButtonType.Cancel).asInstanceOf[Button]
      .onAction = _ => statsTask.cancel()
    dialog.onCloseRequest = _ => statsTask.cancel()

    val dirStatsResult = taskMgr.runTaskAsync(statsTask, new CountStatsProgressMonitor(contentCtrl))

    if (autoClose)
      dirStatsResult.foreach(stats => Platform.runLater { dialog.result = ButtonType.OK })

    val dialogResult = dialog.showAndWait()
    // FIXME: change Try[Option[DirStats]] type maybe to just Option or something - it's too complicated
    dialogResult match {
      case Some(ButtonType.OK) => dirStatsResult.value.get // wtf: this is ugly!
      case Some(ButtonType.Yes) => dirStatsResult.value.get
      case Some(ButtonType.Next) => Success(None)//dirStatsResult.value.get
      case _ => Success(None)
    }
  }
}
