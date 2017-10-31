package org.mikesajak.commander.ui

import javafx.scene.control

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.controller.TabData
import org.mikesajak.commander.ui.controller.ops.DeletePanelController

import scala.util.{Failure, Success}
import scalafx.Includes._
import scalafx.scene.control.ButtonType

class DeleteOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                          countStatsOpCtrl: CountDirStatsOperationCtrl) {
  private val logger = Logger[DeletePanelController]

  private val deleteLayout = "/layout/ops/delete-dialog.fxml"

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
    val fs = targetPath.fileSystem
    fs.delete(targetPath) match {
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

  private def askForDecision(targetPath: VPath): Option[ButtonType] = {
    val (contentPane, contentCtrl) = UILoader.loadScene[DeletePanelController](deleteLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    targetPath match {
      case d: VDirectory =>
        countStatsOpCtrl.runCountDirStats(d, autoClose = true) match {
          case Some(Success(stats)) => contentCtrl.init(d, stats.toString, dialog)
          case Some(Failure(exception)) => contentCtrl.init(d, s"Couldn't count dir stats: $exception", dialog)
          case None => contentCtrl.init(d, "[Skipped counting stats]", dialog)
        }

      case f: VFile =>
        contentCtrl.init(targetPath, null, dialog)
    }
    val result = dialog.showAndWait()
    result.map(jfxbt => new ButtonType(jfxbt.asInstanceOf[control.ButtonType]))
  }
}
