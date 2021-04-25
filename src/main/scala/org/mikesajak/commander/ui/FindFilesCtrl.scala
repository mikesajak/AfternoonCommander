package org.mikesajak.commander.ui

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.custom.CustomListAsDirectory
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.MyScalaFxImplicits._
import org.mikesajak.commander.ui.controller.ops.FindFilesPanelController
import scalafx.Includes._
import scalafx.scene.control.ButtonType
import scribe.Logging

class FindFilesCtrl(appController: ApplicationController,
                    statusMgr: StatusMgr) extends Logging {

  def handleFindAction(): Unit = {
    val findDialogLayout = "/layout/ops/search-files-dialog2.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[FindFilesPanelController](findDialogLayout)

    val dialog = UIUtils.mkModalDialogNoButtonOrder[ButtonType](appController.mainStage, contentPane)

    val selectedPanelDir = statusMgr.selectedTabManager.selectedTab.dir
    val (goToPathButtonType, showAsListButtonType, closeButtonType) = contentCtrl.init(selectedPanelDir, dialog)

    dialog.setWindowSize(600, 400)

    val result = dialog.showAndWait()
    println(s"Find action result: $result")

    contentCtrl.stopSearch()

    result match {
      case Some(goToPathButtonType.delegate) =>
        val selectedResult = contentCtrl.getSelectedResult
        logger.debug(s"Go to path: selected result=$selectedResult")
        selectedResult.foreach { result =>
          val directory = if (result.isDirectory) result.parent.getOrElse(result.directory)
                          else result.directory
          statusMgr.selectedTabManager.selectedTab.controller.setCurrentDirectory(directory, Some(directory))
        }

      case Some(showAsListButtonType.delegate) =>
        val searchResults = contentCtrl.getAllResults
        logger.debug(s"Show as list: all results=$searchResults")
        val resultsListDir = new CustomListAsDirectory("Search results", selectedPanelDir, searchResults)

        statusMgr.selectedTabManager.selectedTab.controller.setCurrentDirectory(resultsListDir)

      case Some(closeButtonType.delegate) => // do nothing

      case _ => // should not even get here
    }

  }

}
