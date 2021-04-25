package org.mikesajak.commander.ui

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.local.LocalFS
import org.mikesajak.commander.fs.{FilesystemsManager, PathToParent, VDirectory, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.OperationType._
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{CopyPanelController, ProgressPanelController}
import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scribe.Logging

import scala.annotation.tailrec
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class OperationUiData(progressDialogType: String, errorDialogType: String, iconName: String)

class TransferOperationController(statusMgr: StatusMgr, appController: ApplicationController,
                                  resourceMgr: ResourceManager, fsMgr: FilesystemsManager,
                                  userDecisionCtrl: UserDecisionCtrl, config: Configuration)
    extends Logging {
  private val copyLayout = "/layout/ops/copy-dialog.fxml"
  private val progressLayout = "/layout/ops/progress-dialog.fxml"

  private val opUiData = Map[OperationType, OperationUiData](
    Copy -> OperationUiData("copy_progress_dialog", "copy_error_dialog", "file-multiple.png"),
    Move -> OperationUiData("move_progress_dialog", "move_error_dialog", "file-multiple.png"))

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
        case Right((targetName, jobStats, dryRun)) =>
          val targetPath = resolveTarget(targetName, selectedTab.dir, sourcePaths.size > 1)
          executeOperation(opType, sourcePaths, targetPath, jobStats, dryRun)
          selectedTab.controller.reload()
          unselectedTab.controller.reload()
        case _ => // skip
      }
    }
  }

  private def resolveTarget(targetPathName: String, sourceDir: VDirectory, forceDir: Boolean) = {
    def isValidDir(name: String) =
      fsMgr.isProperPathPattern(name) || LocalFS.isAbsolutePathPattern(name)

    if (isValidDir(targetPathName)) {
      fsMgr.resolvePath(targetPathName, onlyExisting = false, forceDir = forceDir)
        .getOrElse(throw new IllegalStateException(s"Cannot resolve path: $targetPathName"))
    } else {
      val segments = LocalFS.getPathSegments(targetPathName)
      extendDir(sourceDir, segments, forceDir)
    }
  }

  @tailrec
  private def extendDir(dir: VDirectory, segments: Seq[String], forceDir: Boolean): VPath = {
    segments match {
      case Seq() => dir
      case Seq(lastSegment) =>
        dir.updater.map { upd => if (forceDir || LocalFS.isPathNameDir(lastSegment)) upd.mkChildDirPath(lastSegment)
                               else upd.mkChildFilePath(lastSegment) }
           .getOrElse(throw new IllegalStateException(s"Cannot extend directory: $dir with $segments"))

      case Seq(segment, tail @ _*) =>
        val extendedDir = dir.updater.map(upd => upd.mkChildDirPath(segment))
                             .getOrElse(throw new IllegalStateException(s"Cannot extend directory: $dir with $segments"))

        extendDir(extendedDir, tail, forceDir)
    }
  }

  private def askForDecision(opType: OperationType, sourcePaths: Seq[VPath], targetDir: VDirectory): Either[ButtonType, (String, Option[DirStats], Boolean)] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[CopyPanelController](copyLayout)
    val dialog = UIUtils.mkModalDialog[(String, Boolean)](appController.mainStage, contentPane)

    val statsService = opType match {
      case Copy => contentCtrl.initForCopy(sourcePaths, targetDir, dialog)
      case Move => contentCtrl.initForMove(sourcePaths, targetDir, dialog)
    }

    val result = dialog.showAndWait().asInstanceOf[Option[(String, Boolean)]]

    statsService.cancel()

    result match {
      case Some((targetName, dryRun)) =>
        val stats =
          if (statsService.getState == javafx.concurrent.Worker.State.SUCCEEDED) Some(statsService.value.value)
          else None

        Right((targetName, stats, dryRun))
      case _ =>
        Left(ButtonType.Cancel)
    }
  }

  private def executeOperation(opType: OperationType, sourcePaths: Seq[VPath], targetPath: VPath,
                               jobStats: Option[DirStats], dryRun: Boolean): Unit = {
    logger.debug(s"$opType: $sourcePaths -> $targetPath")

    val result = runOperation(opType, sourcePaths, targetPath, jobStats, dryRun)

    result match {
      case Success(copied) =>
      case Failure(exception) =>
        logger.info(s"Error during $opType operation $sourcePaths -> $targetPath:\n", exception)
        UIUtils.prepareExceptionAlert(appController.mainStage,
                                      resourceMgr.getMessage(s"${opUiData(opType).errorDialogType}.title"),
                                      resourceMgr.getMessage(s"${opUiData(opType).errorDialogType}.header"),
                                      resourceMgr.getMessageWithArgs(s"${opUiData(opType).errorDialogType}.message",
                                                                     Seq(sourcePaths, exception)),
                                      exception)
               .showAndWait()
    }
  }

  private def runOperation(opType: OperationType, srcPaths: Seq[VPath], targetPath: VPath, stats: Option[DirStats], dryRun: Boolean): Try[Boolean] = {
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[IOTaskSummary](appController.mainStage, contentPane)

    val transferJob = TransferJob(srcPaths.map(src => TransferDef(src, targetPath)), opType, preserveModificationDate = true)
    
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
