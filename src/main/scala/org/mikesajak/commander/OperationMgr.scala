package org.mikesajak.commander

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.fs.custom.CustomListAsDirectory
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.MyScalaFxImplicits._
import org.mikesajak.commander.ui._
import org.mikesajak.commander.ui.controller.ops.FindFilesPanelController
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.stage.{Modality, Stage, StageStyle}

class OperationMgr(statusMgr: StatusMgr,
                   resourceMgr: ResourceManager,
                   fsMgr: FilesystemsManager,
                   appController: ApplicationController,
                   copyOperationCtrl: CopyOperationCtrl,
                   mkDirOperationCtrl: MkDirOperationCtrl,
                   deleteOperationCtrl: DeleteOperationCtrl,
                   countDirStatsCtrl: CountDirStatsOperationCtrl) {
  private val logger = Logger(this.getClass)

  def handleView(): Unit = {
    logger.warn(s"handleView - Not implemented yet!")
  }

  def handleEdit(): Unit = {
    logger.warn(s"handleEdit - Not implemented yet!")
  }

  def handleCopy(): Unit = copyOperationCtrl.handleCopy()

  def handleMove(): Unit = {
    logger.warn(s"handleMove - Not implemented yet!")
  }

  def handleMkDir(): Unit = mkDirOperationCtrl.handleMkDir()

  def handleDelete(): Unit = deleteOperationCtrl.handleDelete()

  def handleCountDirStats(): Unit = countDirStatsCtrl.handleCountDirStats()

  def handleExit(): Unit = {
    appController.exitApplication()
  }

  def handleRefreshAction(): Unit = {
    logger.debug(s"Refreshing ${statusMgr.selectedPanel}, ${statusMgr.selectedTabManager.selectedTab.dir}")
    statusMgr.selectedTabManager.selectedTab.controller.reload()
  }

  def handleSettingsAction(): Unit = {
    val settingsLayout = "/layout/settings-panel-layout.fxml"

    val (root, _) = UILoader.loadScene(settingsLayout)
    val stage = new Stage()
    stage.initModality(Modality.ApplicationModal)
    stage.initStyle(StageStyle.Utility)
    stage.initOwner(appController.mainStage)
    stage.scene = new Scene(root, 500, 400)
    stage.show()
  }

  def handleFindAction(): Unit = {
    val findDialogLayout = "/layout/ops/search-files-dialog2.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[FindFilesPanelController](findDialogLayout)

    val dialog = UIUtils.mkModalDialogNoButtonOrder[ButtonType](appController.mainStage, contentPane)
    dialog.title = "Find files..."
    dialog.dialogPane.value.buttonTypes = Seq(FindFilesPanelController.GoToPattButtonType,
                                              FindFilesPanelController.ShowAsListButtonType,
                                              ButtonType.Close)

    val selectedPanelDir = statusMgr.selectedTabManager.selectedTab.dir
    contentCtrl.init(selectedPanelDir, dialog)

    dialog.setWindowSize(600, 400)

    val result = dialog.showAndWait()
    println(s"Find action result: $result")

    contentCtrl.stopSearch()

    result match {
      case Some(FindFilesPanelController.GoToPattButtonType) =>
        val selectedResult = contentCtrl.getSelectedResult
        logger.debug(s"Go to path: selected result=$selectedResult")
        selectedResult.foreach { result =>
          val directory = if (result.isDirectory) result.parent.getOrElse(result.directory)
                          else result.directory
          statusMgr.selectedTabManager.selectedTab.controller.setCurrentDirectory(directory, Some(directory))
        }

      case Some(FindFilesPanelController.ShowAsListButtonType) =>
        val searchResults = contentCtrl.getAllResults
        logger.debug(s"Show as list: all results=$searchResults")
        val resultsListDir = new CustomListAsDirectory("Search results", selectedPanelDir, searchResults)

        statusMgr.selectedTabManager.selectedTab.controller.setCurrentDirectory(resultsListDir)

      case Some(ButtonType.Close) => // do nothing
    }

  }

}
