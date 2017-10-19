package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.PathToParent
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.controller.ops.DeletePanelController

import scalafx.Includes._
import scalafx.scene.control.ButtonType

class DeleteOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController) {
  private val logger = Logger[DeletePanelController]

  def handleDelete(): Unit = {
    val deleteLayout = "/layout/ops/delete-dialog.fxml"
    logger.warn(s"handleDelete - Not fully implemented yet!")

    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val targetPath = selectedTab.controller.selectedPath

    if (!targetPath.isInstanceOf[PathToParent]) {
      val (contentPane, contentCtrl) = UILoader.loadScene[DeletePanelController](deleteLayout)
      val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
      contentCtrl.init(targetPath, dialog)

      val result = dialog.showAndWait()

      result match {
        case Some(ButtonType.Yes) =>
          try {
            logger.debug(s"Deleting: $targetPath")
            val fs = targetPath.fileSystem
            val deleted = fs.delete(targetPath)
            if (!deleted) logger.info(s"Could not delete $targetPath")
            selectedTab.controller.reload()
            // todo: select previous file/directory to keep cursor near deleted dir
          } catch {
            case e: Exception =>
              logger.info(s"Error during deleting $targetPath:\n", e)
              UIUtils.prepareExceptionAlert(appController.mainStage,
                                            "Delete error",
                                            "An error occurred during delete operation.",
                                            s"$targetPath could not be deleted.",
                                            e)
                .showAndWait()
          }
        case _ => // operation cancelled
      }
    }
  }
}
