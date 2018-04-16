package org.mikesajak.commander.ui

import javafx.scene.control

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{CopyPanelController, DeletePanelController, ProgressPanelController}
import org.mikesajak.commander.{ApplicationController, TaskManager}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}
import scalafx.Includes._
import scalafx.scene.control.ButtonType

class CopyOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                        countStatsOpCtrl: CountDirStatsOperationCtrl,
                        resourceMgr: ResourceManager, taskManager: TaskManager) {
  private val logger = Logger[DeletePanelController]

  private val copyLayout = "/layout/ops/copy-dialog.fxml"
  private val progressLayout = "/layout/ops/progress-dialog.fxml"

  def handleCopy(): Unit = {
    logger.warn(s"handleCopy - Not implemented yet!")

    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val sourcePaths = selectedTab.controller.selectedPaths
      .filter(p => !p.isInstanceOf[PathToParent])

    val unselectedTab = statusMgr.unselectedTabManager.selectedTab
    val targetPath = unselectedTab.controller.focusedPath match {
      case p: PathToParent => p.currentDir
      case d: VDirectory => d
      case _ => unselectedTab.dir
    }

    if (sourcePaths.nonEmpty) {
      // TODO: count stats
      val result = askForDecision(sourcePaths, targetPath)

      println(s"Copy dialog decision: $result")

      // TODO: show stats and ask for decision

      // TODO: execute copy
//      runCopyOperation(sourcePaths, targetPath, None)
    }

  }

  private def countPathStats(path: VPath): Try[Option[DirStats]] =
    path match {
      case d: VDirectory => countStatsOpCtrl.runCountDirStats(List(d), autoClose = false)
      case f: VFile => Success(Some(DirStats(1, 0, f.size, 0)))
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

    Success(false) // FIXME: evaluate the result of operation and return proper value
  }

  private def askForDecision(sourcePaths: Seq[VPath], targetDir: VDirectory): Option[(ButtonType, Option[DirStats])] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[CopyPanelController](copyLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    val statsCountOp = countStatsOpCtrl.runCountDirStats(sourcePaths, contentCtrl)

    contentCtrl.init(sourcePaths, targetDir, DirStats.Empty, dialog)

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
