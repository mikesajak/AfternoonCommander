package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import javafx.scene.control
import org.mikesajak.commander.fs.{PathToParent, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.{DirStats, RecursiveDeleteTask}
import org.mikesajak.commander.ui.controller.ops.{DeletePanelController, ProgressPanelController}
import org.mikesajak.commander.{ApplicationController, TaskManager}
import scalafx.Includes._
import scalafx.scene.control.ButtonType

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class DeleteOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                          countStatsOpCtrl: CountDirStatsOperationCtrl,
                          resourceMgr: ResourceManager, taskManager: TaskManager) {
  private val logger = Logger[DeletePanelController]

  private val deleteLayout = "/layout/ops/delete-dialog.fxml"
  private val progressLayout = "/layout/ops/progress-dialog.fxml"

  def handleDelete(): Unit = {
    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val targetPaths = selectedTab.controller.selectedPaths
      .filter(p => !p.isInstanceOf[PathToParent])

    if (targetPaths.nonEmpty) {
      val result = askForDecision(targetPaths)

      println(s"Delete confirm decision: $result")

      result match {
        case Some((ButtonType.Yes, stats)) =>
          executeDelete(targetPaths, stats)
          selectedTab.controller.reload()
        case _ => // operation cancelled
      }
    }
  }

  private def executeDelete(targetPath: Seq[VPath], stats: Option[DirStats]): Unit = {
    logger.debug(s"Deleting: $targetPath")

    val deleteResult = runDeleteOperation(targetPath, stats)

    deleteResult match {
      case Success(deleted) =>
      // todo: select previous file/directory to keep cursor near deleted dir
      case Failure(exception) =>
        logger.info(s"Error during deleting $targetPath:\n", exception)
        UIUtils.prepareExceptionAlert(appController.mainStage,
                                      "Delete error", // TODO: i18
                                      s"An error occurred during delete operation", // TODO: i18
                                      s"$targetPath could not be deleted, because of an error: $exception.", // TODO: i18
                                      exception)
          .showAndWait()
    }
  }

  private def runDeleteOperation(paths: Seq[VPath], stats: Option[DirStats]): Try[Boolean] = {
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
    val (pathType, pathName) =
      paths match {
        case p if p.size == 1 && p.head.isDirectory => ("directory and all its contents", "${p.head}") // TODO: i18
        case p if p.size == 1 => ("file", "${p.head") // TODO: i18
        case p @ _ => (s"paths", s"${p.size} elements") // TODO: i18
      }

    val deleteTask = new RecursiveDeleteTask(paths, stats)

    ctrl.init(s"Delete", s"Delete selected $pathType\n$pathName", // TODO: i18
              s"Deleting $pathName", s"$pathName", resourceMgr.getIcon("delete-circle-48.png"), // TODO: i18
              progressDialog, deleteTask)

    taskManager.runTaskAsync(deleteTask, new ProgressMonitorWithGUIPanel(ctrl))
    val result = progressDialog.showAndWait()

    Success(false) // FIXME: evaluate the result of operation and return proper value
  }

  private def askForDecision(targetPaths: Seq[VPath]): Option[(ButtonType, Option[DirStats])] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[DeletePanelController](deleteLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    val statsCountOp = countStatsOpCtrl.runCountDirStats(targetPaths, contentCtrl)

    contentCtrl.init(targetPaths, DirStats.Empty, dialog)

    val result = dialog.showAndWait()
    statsCountOp.requestAbort()
    val stats =
      if (statsCountOp.finalStatus.isCompleted) Await.result(statsCountOp.finalStatus, Duration.Zero)
      else None

    result
      .map(jfxbt => new ButtonType(jfxbt.asInstanceOf[control.ButtonType]))
      .map(bt => (bt, stats))
  }
}
