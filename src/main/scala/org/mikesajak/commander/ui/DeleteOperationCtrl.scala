package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import javafx.scene.control
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.{PathToParent, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{DeletePanelController, ProgressPanelController}
import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

import scala.util.{Failure, Success, Try}

class DeleteOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                          countStatsOpCtrl: CountDirStatsOperationCtrl,
                          userDecisionCtrl: UserDecisionCtrl, resourceMgr: ResourceManager) {
  private val logger = Logger[DeletePanelController]

  private val deleteLayout = "/layout/ops/delete-dialog.fxml"
  private val progressLayout = "/layout/ops/progress-dialog.fxml"

  def handleDelete(): Unit = {
    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val targetPaths = selectedTab.controller.selectedPaths
      .filter(p => !p.isInstanceOf[PathToParent])

    if (targetPaths.nonEmpty) {
      val result = askForDecision(targetPaths)

      logger.debug(s"Delete confirm decision: $result")

      result match {
        case Some((ButtonType.Yes, stats, dryRun)) =>
          executeDelete(targetPaths, stats, dryRun)
          selectedTab.controller.reload()
        case _ => // operation cancelled
      }
    }
  }

  private def executeDelete(targetPaths: Seq[VPath], stats: Option[DirStats], dryRun: Boolean): Unit = {
    logger.debug(s"Deleting: $targetPaths")

    val deleteResult = runDeleteOperation(targetPaths, stats, dryRun)

    deleteResult match {
      case Success(deleted) =>
      // todo: select previous file/directory to keep cursor near deleted dir
      case Failure(exception) =>
        logger.info(s"Error during deleting $targetPaths:\n", exception)
        UIUtils.prepareExceptionAlert(appController.mainStage,
                                      resourceMgr.getMessage("delete_error_dialog.title"),
                                      resourceMgr.getMessage("delete_error_dialog.header"),
                                      resourceMgr.getMessageWithArgs("delete_error_dialog.message",
                                                                     Seq(targetPaths, exception)),
                                      exception)
          .showAndWait()
    }
  }

  private def runDeleteOperation(paths: Seq[VPath], stats: Option[DirStats], dryRun: Boolean): Try[Boolean] = {
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[IOTaskSummary](appController.mainStage, contentPane)

    val (headerText, statusMessage) = paths match {
      case p if p.size == 1 && p.head.isDirectory =>
        (resourceMgr.getMessageWithArgs("delete_progress_dialog.header.directory", Seq(p.head)),
            resourceMgr.getMessageWithArgs("delete_progress_dialog.status.message", Seq(p.head)))
      case p if p.size == 1 =>
        (resourceMgr.getMessageWithArgs("delete_progress_dialog.header.file", Seq(p.head)),
            resourceMgr.getMessageWithArgs("delete_progress_dialog.status.message", Seq(p.head)))
      case p @ _ =>
        (resourceMgr.getMessageWithArgs("delete_progress_dialog.header.paths", Seq(p.size)),
            resourceMgr.getMessageWithArgs("delete_progress_dialog.status.message.paths", Seq(p.size)))
    }

    val jobs = paths.map(p => DeleteJobDef(p))

    val deleteService = new BackgroundService(new RecursiveDeleteTask(jobs, stats, dryRun, userDecisionCtrl, resourceMgr))

    ctrl.init(resourceMgr.getMessage("delete_progress_dialog.title"),
              headerText, statusMessage,
              resourceMgr.getIcon("delete-circle.png", IconSize.Big),
              progressDialog, deleteService)

    val result = progressDialog.showAndWait()

    result match {
      case Some(summary: IOTaskSummary) if summary.errors.nonEmpty =>
        new Alert(AlertType.Error) {
          initOwner(appController.mainStage)
          title = resourceMgr.getMessage("delete_error_dialog.title")
          headerText = resourceMgr.getMessage("delete_error_dialog.header")
          contentText = summary.errors.map(err => s"${err._1}: ${err._2}")
                               .mkString("\n")
        }.showAndWait()
      case _ =>
    }

    Success(false) // FIXME: evaluate the result of operation and return proper value
  }

  private def askForDecision(targetPaths: Seq[VPath]): Option[(ButtonType, Option[DirStats], Boolean)] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[DeletePanelController](deleteLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    val statsService = contentCtrl.init(targetPaths, DirStats.Empty, dialog)

    val result = dialog.showAndWait()
    statsService.cancel()

    val stats =
      if (statsService.getState == javafx.concurrent.Worker.State.SUCCEEDED) Some(statsService.value.value)
      else None

    result
      .map(jfxbt => new ButtonType(jfxbt.asInstanceOf[control.ButtonType]))
      .map(bt => (bt, stats, contentCtrl.dryRunSelected))
  }
}
