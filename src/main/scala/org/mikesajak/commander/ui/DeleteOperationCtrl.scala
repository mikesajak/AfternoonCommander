package org.mikesajak.commander.ui

import javafx.scene.control

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.{ProgressMonitor, RecursiveDeleteTask}
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
      val result = askForDecision(targetPath)

      println(s"Delete confirm decision: $result")

      result match {
        case Some(ButtonType.Yes) => executeDelete(targetPath, selectedTab)
        case _ => // operation cancelled
      }
    }
  }

  private def executeDelete(targetPath: VPath, selectedTab: TabData): Unit = {
    logger.debug(s"Deleting: $targetPath")

    val deleteResult = runDeleteOperation(targetPath)

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

  private def runDeleteOperation(path: VPath): Try[Boolean] = {
//    val fs = path.fileSystem
//    fs.delete(path)
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
    val pathType = if (path.isDirectory) "directory and all its contents" else "file"

    val deleteTask = new RecursiveDeleteTask(path)

    ctrl.init(s"Delete", s"Delete selected $pathType\n$path",
              s"Deleting $path", s"$path", resourceMgr.getIcon("delete-circle-48.png"),
              progressDialog, deleteTask)

    // FIXME: run delete task!!
    taskManager.runTaskAsync(deleteTask, new ProgressMonitorWithGUIPanel(ctrl))
//    val result = progressDialog.showAndWait()

    Success(false)
  }

  class ProgressMonitorWithGUIPanel(progressPanelController: ProgressPanelController) extends ProgressMonitor[Unit] {
    override def notifyProgressIndeterminate(message: Option[String], state: Option[Unit]): Unit =
      Platform.runLater {
        progressPanelController.updateIndeterminate(message.getOrElse(""))
      }

    override def notifyProgress(progress: Float, message: Option[String], state: Option[Unit]): Unit =
      Platform.runLater {
        progressPanelController.update(message.getOrElse(""), progress)
      }

    override def notifyFinished(message: Option[String], state: Option[Unit]): Unit =
      Platform.runLater {
        progressPanelController.updateFinished(message.getOrElse(""))
      }

    override def notifyError(message: String, state: Option[Unit]): Unit = ??? // TODO

    override def notifyAborted(message: String): Unit = ??? // TODO
  }

  private def askForDecision(targetPath: VPath): Option[ButtonType] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[DeletePanelController](deleteLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    targetPath match {
      case d: VDirectory =>
        val dirStats = countStatsOpCtrl.runCountDirStats(d, autoClose = true)
        contentCtrl.init(d, dirStats, dialog)

      case f: VFile =>
        contentCtrl.init(targetPath, Success(None), dialog)
    }
    val result = dialog.showAndWait()
    result.map(jfxbt => new ButtonType(jfxbt.asInstanceOf[control.ButtonType]))
  }
}
