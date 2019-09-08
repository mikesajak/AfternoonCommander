package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import javafx.scene.control
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{CopyPanelController, DeletePanelController, ProgressPanelController}
import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

import scala.util.{Failure, Success, Try}

class CopyOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                        countStatsOpCtrl: CountDirStatsOperationCtrl, resourceMgr: ResourceManager) {
  private val logger = Logger[DeletePanelController]

  private val copyLayout = "/layout/ops/copy-dialog.fxml"
  private val progressLayout = "/layout/ops/progress-dialog.fxml"

  def handleCopy(): Unit = {
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

      val result = askForDecision(sourcePaths, targetDir)

      logger.debug(s"Copy dialog decision: $result")

      result match {
        case Right((jobStats, dryRun)) =>
          executeCopy(sourcePaths, targetDir, jobStats, dryRun)
          selectedTab.controller.reload()
          unselectedTab.controller.reload()
        case _ => // skip
      }
    }
  }

  private def executeCopy(sourcePaths: Seq[VPath], targetDir: VDirectory, jobStats: Option[DirStats], dryRun: Boolean): Unit = {
    logger.debug(s"Copying: $sourcePaths -> $targetDir")

    val copyResult = runCopyOperation(sourcePaths, targetDir, jobStats, dryRun)

    copyResult match {
      case Success(copied) =>
      case Failure(exception) =>
        logger.info(s"Error during copy operation $sourcePaths -> $targetDir:\n", exception)
        UIUtils.prepareExceptionAlert(appController.mainStage,
                                      resourceMgr.getMessage("copy_error_dialog.title"),
                                      resourceMgr.getMessage("copy_error_dialog.header"),
                                      resourceMgr.getMessageWithArgs("copy_error_dialog.message",
                                                                     Seq(sourcePaths, exception)),
                                      exception)
               .showAndWait()
    }
  }

  private def askForDecision(sourcePaths: Seq[VPath], targetDir: VDirectory): Either[ButtonType, (Option[DirStats], Boolean)] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[CopyPanelController](copyLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    val statsService = contentCtrl.init(sourcePaths, targetDir, dialog)

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

  private def runCopyOperation(srcPaths: Seq[VPath], targetDir: VDirectory, stats: Option[DirStats], dryRun: Boolean): Try[Boolean] = {
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[IOTaskSummary](appController.mainStage, contentPane)

    val copyJobDefs = srcPaths.map(src => CopyJobDef(src, targetDir))

    val (headerText, statusMessage) = srcPaths match {
      case p if p.size == 1 && p.head.isDirectory =>
        (resourceMgr.getMessageWithArgs("copy_progress_dialog.header.directory", Seq(p.head)),
        resourceMgr.getMessageWithArgs("copy_progress_dialog.status.message", Seq(p.head)))
      case p if p.size == 1 =>
        (resourceMgr.getMessageWithArgs("copy_progress_dialog.header.file", Seq(p.head)),
        resourceMgr.getMessageWithArgs("copy_progress_dialog.status.message", Seq(p.head)))
      case p @ _ =>
        (resourceMgr.getMessageWithArgs("copy_progress_dialog.header.paths", Seq(p.size)),
        resourceMgr.getMessageWithArgs("copy_progress_dialog.status.message.paths", Seq(p.size)))
    }

    val copyService = new BackgroundService(new RecursiveCopyTask(copyJobDefs, stats, dryRun))

    ctrl.init(resourceMgr.getMessage("copy_progress_dialog.title"), headerText, statusMessage,
              resourceMgr.getIcon("file-multiple.png", IconSize.Big),
      progressDialog, copyService)

    val result = progressDialog.showAndWait()

    result match {
      case Some(summary: IOTaskSummary) if summary.errors.nonEmpty =>
        new Alert(AlertType.Error) {
          initOwner(appController.mainStage)
          title = resourceMgr.getMessage("copy_error_dialog.title")
          headerText = resourceMgr.getMessage("copy_error_dialog.header")
          contentText = summary.errors.map(err => s"${err._1}: ${err._2}")
                               .reduce((a,b) => s"$a\n$b")
        }.showAndWait()
      case _ =>
    }

    Success(false) // FIXME: evaluate the result of operation and return proper value
  }

}
