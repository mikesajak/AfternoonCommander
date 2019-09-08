package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.controller.ops.MkDirPanelController
import scalafx.Includes._

class MkDirOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController) {
  private val logger = Logger[MkDirOperationCtrl]

  def handleMkDir(): Unit = {
    val curTab = statusMgr.selectedTabManager.selectedTab
    val contentLayout = "/layout/ops/mkdir-dialog.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[MkDirPanelController](contentLayout)
    val selectedTab = statusMgr.selectedTabManager.selectedTab

    val dialog = UIUtils.mkModalDialog[String](appController.mainStage, contentPane)
    contentCtrl.init(selectedTab.dir, dialog)

    val result = dialog.showAndWait().asInstanceOf[Option[String]]

    result foreach { newDirName =>
      try {
        logger.debug(s"Creating directory $newDirName, in parent directory: ${selectedTab.dir}")
        val newDir = selectedTab.dir.updater
            .map(_.mkChildDir(newDirName))
            .getOrElse(throw new MkDirException("Cannot create directory $newDirName. Target directory ${selectedTab.dir} is not writable."))
        selectedTab.controller.reload()
        selectedTab.controller.setTableFocusOn(Some(newDir))
      } catch {
        case e: Exception =>
          logger.info(s"Error during creating directory $newDirName in parent dir ${selectedTab.dir}", e)
          UIUtils.prepareExceptionAlert(appController.mainStage,
                                        "Create folder error",
                                        "An error occurred during create folder operation.",
                                        s"$newDirName could not be created in ${selectedTab.dir}.\n${e.getLocalizedMessage}",
                                        e)
            .showAndWait()
      }
    }
  }

  class MkDirException(msg: String) extends Exception(msg)
}
