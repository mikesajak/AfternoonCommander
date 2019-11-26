package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import javafx.scene.control
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.OperationType._
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{CopyPanelController, DeletePanelController, ProgressPanelController}
import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

import scala.util.{Failure, Success, Try}

case class OperationUiData(progressDialogType: String, errorDialogType: String, iconName: String)

class TransferOperationController(statusMgr: StatusMgr, appController: ApplicationController,
                                  countStatsOpCtrl: CountDirStatsOperationCtrl, resourceMgr: ResourceManager,
                                  userDecisionCtrl: UserDecisionCtrl, config: Configuration) {
  private val logger = Logger[DeletePanelController]

  private val copyLayout = "/layout/ops/copy-dialog.fxml"
  private val progressLayout = "/layout/ops/progress-dialog.fxml"

  private val opUiData = Map[OperationType, OperationUiData](
    Copy -> OperationUiData("copy_progress_dialog", "copy_error_dialog", "file-multiple.png"),
    Move -> OperationUiData("move_progress_dialog", "move_error_dialog", "file-miltiple.png"))

  def handleOperation(opType: OperationType): Unit = {
    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val sourcePaths = selectedTab.controller.selectedPaths
      .filter(p => !p.isInstanceOf[PathToParent])

    if (sourcePaths.nonEmpty) {
      val unselectedTab = statusMgr.unselectedTabManager.selectedTab
      val targetDir = unselectedTab.controller.focusedPath match {
        case p: PathToParent => p.currentDir
        case d: VDirectory => d
        case _ => unselectedTab.dir
      }

      val result = askForDecision(opType, sourcePaths, targetDir)

      logger.debug(s"$opType dialog decision: $result")

      result match {
        case Right((jobStats, dryRun)) =>
          executeOperation(opType, sourcePaths, targetDir, jobStats, dryRun)
          selectedTab.controller.reload()
          unselectedTab.controller.reload()
        case _ => // skip
      }
    }
  }

  private def askForDecision(opType: OperationType, sourcePaths: Seq[VPath], targetDir: VDirectory): Either[ButtonType, (Option[DirStats], Boolean)] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[CopyPanelController](copyLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    val statsService = opType match {
      case Copy => contentCtrl.initForCopy(sourcePaths, targetDir, dialog)
      case Move => contentCtrl.initForMove(sourcePaths, targetDir, dialog)
    }

    val result = dialog.showAndWait()

    statsService.cancel()

    result match {
      case Some(bt) if bt == control.ButtonType.YES || bt == control.ButtonType.OK =>
        val stats =
          if (statsService.getState == javafx.concurrent.Worker.State.SUCCEEDED) Some(statsService.value.value)
          else None

        Right(stats, contentCtrl.dryRunSelected)
      case Some(bt) => Left(new ButtonType(bt.asInstanceOf[control.ButtonType]))
      case _ => Left(ButtonType.Cancel)
    }
  }

  private def executeOperation(opType: OperationType, sourcePaths: Seq[VPath], targetDir: VDirectory, jobStats: Option[DirStats], dryRun: Boolean): Unit = {
    logger.debug(s"$opType: $sourcePaths -> $targetDir")

    val result = runOperation(opType, sourcePaths, targetDir, jobStats, dryRun)

    result match {
      case Success(copied) =>
      case Failure(exception) =>
        logger.info(s"Error during $opType operation $sourcePaths -> $targetDir:\n", exception)
        UIUtils.prepareExceptionAlert(appController.mainStage,
                                      resourceMgr.getMessage(s"${opUiData(opType).errorDialogType}.title"),
                                      resourceMgr.getMessage(s"${opUiData(opType).errorDialogType}.header"),
                                      resourceMgr.getMessageWithArgs(s"${opUiData(opType).errorDialogType}.message",
                                                                     Seq(sourcePaths, exception)),
                                      exception)
               .showAndWait()
    }
  }

  private def runOperation(opType: OperationType, srcPaths: Seq[VPath], targetDir: VDirectory, stats: Option[DirStats], dryRun: Boolean): Try[Boolean] = {
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[IOTaskSummary](appController.mainStage, contentPane)

    val transferJob = TransferJob(srcPaths.map(src => TransferDef(src, targetDir)), opType, preserveModificationDate = true)
    
    val (headerText, statusMessage) = srcPaths match {
      case p if p.size == 1 && p.head.isDirectory =>
        (resourceMgr.getMessageWithArgs(s"${opUiData(opType).progressDialogType}.header.directory", Seq(p.head)),
        resourceMgr.getMessageWithArgs(s"${opUiData(opType).progressDialogType}.status.message", Seq(p.head)))
      case p if p.size == 1 =>
        (resourceMgr.getMessageWithArgs(s"${opUiData(opType).progressDialogType}.header.file", Seq(p.head)),
        resourceMgr.getMessageWithArgs(s"${opUiData(opType).progressDialogType}.status.message", Seq(p.head)))
      case p @ _ =>
        (resourceMgr.getMessageWithArgs(s"${opUiData(opType).progressDialogType}.header.paths", Seq(p.size)),
        resourceMgr.getMessageWithArgs(s"${opUiData(opType).progressDialogType}.status.message.paths", Seq(p.size)))
    }

    val service = new BackgroundService(new RecursiveTransferTask(transferJob, stats, dryRun, config, userDecisionCtrl))

    ctrl.init(resourceMgr.getMessage(s"${opUiData(opType).progressDialogType}.title"), headerText, statusMessage,
              resourceMgr.getIcon(opUiData(opType).iconName, IconSize.Big), progressDialog, service)

    val result = progressDialog.showAndWait()

    result match {
      case Some(summary: IOTaskSummary) if summary.errors.nonEmpty =>
        new Alert(AlertType.Error) {
          initOwner(appController.mainStage)
          title = resourceMgr.getMessage(s"${opUiData(opType).errorDialogType}.title")
          headerText = resourceMgr.getMessage(s"${opUiData(opType).errorDialogType}.header")
          contentText = summary.errors.map(err => s"${err._1}: ${err._2}")
                               .reduce((a,b) => s"$a\n$b")
        }.showAndWait()
      case _ =>
    }

    Success(false) // FIXME: evaluate the result of operation and return proper value
  }

}
