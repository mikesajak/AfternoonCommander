package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import javafx.scene.control
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{CopyPanelController, DeletePanelController, ProgressPanelController}
import org.mikesajak.commander.{ApplicationController, TaskManager}
import scalafx.Includes._
import scalafx.scene.control.ButtonType

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class CopyOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                        countStatsOpCtrl: CountDirStatsOperationCtrl,
                        resourceMgr: ResourceManager, taskManager: TaskManager) {
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
        case Right(jobStats) =>
          executeCopy(sourcePaths, targetDir, jobStats)
          selectedTab.controller.reload()
          unselectedTab.controller.reload()
        case _ => // skip
      }
    }

  }

  private def executeCopy(sourcePaths: Seq[VPath], targetDir: VDirectory, jobStats: Option[DirStats]): Unit = {
    logger.debug(s"Copying: $sourcePaths -> $targetDir")

    val copyResult = runCopyOperation(sourcePaths, targetDir, jobStats)

    copyResult match {
      case Success(copied) =>
      case Failure(exception) =>
        logger.info(s"Error during copy operation $sourcePaths -> $targetDir:\n", exception)
        UIUtils.prepareExceptionAlert(appController.mainStage,
          "Copy error", // TODO: i18
          s"An error occurred during copy operation", // TODO: i18
          s"Some of the paths $sourcePaths could not be copied successfully, because of an error: $exception.", // TODO: i18
          exception)
          .showAndWait()
    }
  }

  private def askForDecision(sourcePaths: Seq[VPath], targetDir: VDirectory): Either[ButtonType, Option[DirStats]] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[CopyPanelController](copyLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    val statsCountOp = countStatsOpCtrl.runCountDirStats(sourcePaths, contentCtrl)

    contentCtrl.init(sourcePaths, targetDir, DirStats.Empty, dialog)

    val result = dialog.showAndWait()
    statsCountOp.requestAbort()

    result match {
      case Some(bt) if bt == control.ButtonType.YES || bt == control.ButtonType.OK =>
        val stats =
          if (statsCountOp.finalStatus.isCompleted) Await.result(statsCountOp.finalStatus, Duration.Zero)
          else None
        Right(stats)
      case Some(bt) => Left(new ButtonType(bt.asInstanceOf[control.ButtonType]))
      case _ => Left(ButtonType.Cancel)
    }
  }

  private def runCopyOperation(srcPaths: Seq[VPath], targetDir: VDirectory, stats: Option[DirStats]): Try[Boolean] = {
    val (contentPane, ctrl) = UILoader.loadScene[ProgressPanelController](progressLayout)

    val progressDialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
    val (pathType, pathName) =
      srcPaths match {
        case p if p.size == 1 && p.head.isDirectory => ("directory and all its contents", s"${p.head}")
        case p if p.size == 1 => ("file", s"${p.head}")
        case p @ _ => (s"paths", s"${p.size} elements")
      }

    val copyJobDefs = srcPaths.map(src => CopyJobDef(src, targetDir))

    val copyTask = new CopyMultiFilesTask(copyJobDefs, stats)

    ctrl.init(s"Copy", s"Copy selected $pathType\n$pathName",
      s"Copying $pathName", s"$pathName", resourceMgr.getIcon("delete-circle-48.png"),
      progressDialog, copyTask)

    taskManager.runTaskAsync(copyTask, new MultiProgressMonitor(List(new ConsoleProgressMonitor[IOTaskSummary](),
                                                                     new ProgressMonitorWithGUIPanel(ctrl))))
    val result = progressDialog.showAndWait()

    result
    Success(false) // FIXME: evaluate the result of operation and return proper value
  }

}
