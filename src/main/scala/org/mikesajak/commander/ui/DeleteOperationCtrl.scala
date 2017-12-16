package org.mikesajak.commander.ui

import javafx.scene.control

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.{DirStats, IOTaskSummary, ProgressMonitor, RecursiveDeleteTask}
import org.mikesajak.commander.ui.controller.TabData
import org.mikesajak.commander.ui.controller.ops.{DeletePanelController, ProgressPanelController}
import org.mikesajak.commander.{ApplicationController, TaskManager}

import scala.util.{Failure, Success, Try}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.ButtonType

class DeleteOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                          countStatsOpCtrl: CountDirStatsOperationCtrl,
                          resourceMgr: ResourceManager, taskManager: TaskManager) {
  private val logger = Logger[DeletePanelController]

  private val deleteLayout = "/layout/ops/delete-dialog.fxml"
  private val progressLayout = "/layout/ops/progress-dialog.fxml"

  def handleDelete(): Unit = {
    logger.warn(s"handleDelete - Not fully implemented yet!")

    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val targetPath = selectedTab.controller.selectedPath

    if (!targetPath.isInstanceOf[PathToParent]) {
      var statsResult = countPathStats(targetPath)

      val result = askForDecision(targetPath, statsResult)

      println(s"Delete confirm decision: $result")

      result match {
        case Some(ButtonType.Yes) =>
          executeDelete(targetPath, selectedTab, statsResult.getOrElse(None))
        case _ => // operation cancelled
      }
    }
  }

  private def executeDelete(targetPath: VPath, selectedTab: TabData, stats: Option[DirStats]): Unit = {
    logger.debug(s"Deleting: $targetPath")

    val deleteResult = runDeleteOperation(targetPath, stats)

    deleteResult match {
      case Success(deleted) =>
      // todo: select previous file/directory to keep cursor near deleted dir
      case Failure(exception) =>
        logger.info(s"Error during deleting $targetPath:\n", exception)
        UIUtils.prepareExceptionAlert(appController.mainStage,
                                      "Delete error",
                                      s"An error occurred during delete operation",
                                      s"$targetPath could not be deleted, because of an error: $exception.",
                                      exception)
          .showAndWait()
    }
    selectedTab.controller.reload()
  }

  private def runDeleteOperation(path: VPath, stats: Option[DirStats]): Try[Boolean] = {
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
    val pathType = if (path.isDirectory) "directory and all its contents" else "file"

    val deleteTask = new RecursiveDeleteTask(path, stats)

    ctrl.init(s"Delete", s"Delete selected $pathType\n$path",
              s"Deleting $path", s"$path", resourceMgr.getIcon("delete-circle-48.png"),
              progressDialog, deleteTask)

    taskManager.runTaskAsync(deleteTask, new ProgressMonitorWithGUIPanel(ctrl))
    val result = progressDialog.showAndWait()

    Success(false) // FIXME: evaluate the result of operation and return proper value
  }

  class ProgressMonitorWithGUIPanel(progressPanelController: ProgressPanelController) extends ProgressMonitor[IOTaskSummary] {
    override def notifyProgressIndeterminate(message: Option[String], state: Option[IOTaskSummary]): Unit =
      Platform.runLater {
        progressPanelController.updateIndeterminate(message.getOrElse(""))
      }

    override def notifyProgress(progress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
      Platform.runLater {
        progressPanelController.update(message.getOrElse(""), progress)
      }

    override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
      Platform.runLater {
        progressPanelController.update(message.getOrElse(""), partProgress, totalProgress)
      }

    override def notifyFinished(message: Option[String], state: Option[IOTaskSummary]): Unit =
      Platform.runLater {
        progressPanelController.updateFinished(message.getOrElse(""))
      }

    override def notifyError(message: String, state: Option[IOTaskSummary]): Unit = ???
      // TODO: ask if continue or abort?

    override def notifyAborted(message: String): Unit =
      Platform.runLater {
        progressPanelController.updateAborted(message)
      }
  }

  private def countPathStats(path: VPath): Try[Option[DirStats]] =
    path match {
      case d: VDirectory => countStatsOpCtrl.runCountDirStats(d, autoClose = true)
      case f: VFile => Success(Some(DirStats(1, 0, f.size, 0)))
    }

  private def askForDecision(targetPath: VPath, stats: Try[Option[DirStats]]): Option[ButtonType] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[DeletePanelController](deleteLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    targetPath match {
      case d: VDirectory =>
        contentCtrl.init(d, stats, dialog)

      case _: VFile =>
        contentCtrl.init(targetPath, Success(None), dialog)
    }

    val result = dialog.showAndWait()
    result.map(jfxbt => new ButtonType(jfxbt.asInstanceOf[control.ButtonType]))
  }
}
