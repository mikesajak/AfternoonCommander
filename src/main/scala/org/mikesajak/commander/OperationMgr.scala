package org.mikesajak.commander

import com.typesafe.scalalogging.Logger
import javafx.stage
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.status.StatusMgr
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
    val findDialogLayout = "/layout/ops/search-files-dialog.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[FindFilesPanelController](findDialogLayout)

    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)
    dialog.title = "Find files..."
    val window = dialog.getDialogPane.getScene.getWindow.asInstanceOf[stage.Stage]
    window.setMinHeight(600)
    window.setMinWidth(400)

    dialog.dialogPane.value.buttonTypes = Seq(ButtonType.Close)
    val result = dialog.showAndWait()

    println(s"Find action result: $result")
  }

}
