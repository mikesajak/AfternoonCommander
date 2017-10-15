package org.mikesajak.commander

import javafx.stage

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{FilesystemsManager, PathToParent, VDirectory}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{CountStatsPanelController, DeletePanelController, FindFilesPanelController, MkDirPanelController}
import org.mikesajak.commander.ui.{ResourceManager, UILoader, UIUtils}

import scalafx.Includes._
import scalafx.scene.control._
import scalafx.scene.{Parent, Scene}
import scalafx.stage.{Modality, Stage, StageStyle}

class OperationMgr(statusMgr: StatusMgr,
                   resourceMgr: ResourceManager,
                   fsMgr: FilesystemsManager,
                   taskManager: TaskManager,
                   appController: ApplicationController) {
  private val logger = Logger(this.getClass)

  def handleView(): Unit = {
    logger.warn(s"handleView - Not implemented yet!")
  }

  def handleEdit(): Unit = {
    logger.warn(s"handleEdit - Not implemented yet!")
  }

  def handleCopy(): Unit = {
    logger.warn(s"handleCopy - Not implemented yet!")
  }

  def handleMove(): Unit = {
    logger.warn(s"handleMove - Not implemented yet!")
  }

  def handleMkDir(): Unit = {
    val curTab = statusMgr.selectedTabManager.selectedTab
    val contentLayout = "/layout/ops/mkdir-dialog.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[MkDirPanelController](contentLayout)
    val selectedTab = statusMgr.selectedTabManager.selectedTab

    val dialog = mkModalDialog[String](contentPane)
    contentCtrl.init(selectedTab.dir, dialog)

    val result = dialog.showAndWait().asInstanceOf[Option[String]]

    result foreach { newDirName =>
      try {
        logger.debug(s"Creating directory $newDirName, in parent directory: ${selectedTab.dir}")
        selectedTab.dir.mkChildDir(newDirName)
        selectedTab.controller.reload()
        selectedTab.controller.select(newDirName)
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

  def handleDelete(): Unit = {
    val deleteLayout = "/layout/ops/delete-dialog.fxml"
    logger.warn(s"handleDelete - Not fully implemented yet!")

    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val targetPath = selectedTab.controller.selectedPath

    if (!targetPath.isInstanceOf[PathToParent]) {
      val (contentPane, contentCtrl) = UILoader.loadScene[DeletePanelController](deleteLayout)
      val dialog = mkModalDialog[ButtonType](contentPane)
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

  def handleCountDirStats(): Unit = {
    val contentLayout = "/layout/ops/count-stats-dialog.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[CountStatsPanelController](contentLayout)
    val selectedTab = statusMgr.selectedTabManager.selectedTab

    val dialog = mkModalDialog[ButtonType](contentPane)
    dialog.title = "Afternoon commander"

    contentCtrl.init(selectedTab.dir, dialog, showClose = true, showCancel = true, showSkip = false)
    contentCtrl.updateButtons(enableClose = false, enableCancel = true, enableSkip = false)

    Option(statusMgr.selectedTabManager.selectedTab.controller.selectedRow)
      .map(_.path) match {
        case Some(selectedPath) =>
          if (selectedPath.isDirectory && !selectedPath.isInstanceOf[PathToParent]) {
            val selDir = selectedPath.asInstanceOf[VDirectory]
//            taskManager.runTaskAsync(new DirStatsTask(selDir), new ConsoleProgressMonitor2[DirStats])
            taskManager.runTaskAsync(new DirStatsTask(selDir), new CountStatsProgressMonitor(contentCtrl))
          } else {
            println(s"Cannot run count dir stats on file: $selectedPath")
          }
        case None => println(s"No directory is selected")
      }

    val result = dialog.showAndWait()
  }

  def handleTestTask(sync: Boolean): Unit = {
    if (sync) taskManager.runTaskSync(new TestTask(), new ConsoleProgressMonitor2[Unit])
    else      taskManager.runTaskAsync(new TestTask(), new ConsoleProgressMonitor2[Unit])
  }

  def handleExit(): Unit = {
    appController.exitApplication()
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

    val dialog = mkModalDialog[ButtonType](contentPane)
    dialog.title = "Find files..."
    val window = dialog.getDialogPane.getScene.getWindow.asInstanceOf[stage.Stage]
    window.setMinHeight(600)
    window.setMinWidth(400)

    dialog.dialogPane.value.buttonTypes = Seq(ButtonType.Close)
    val result = dialog.showAndWait()

    println(s"Find action result: $result")
  }

  private def mkModalDialog[ResultType](content: Parent) = new Dialog[ResultType]() {
    initOwner(appController.mainStage)
    initStyle(StageStyle.Utility)
    initModality(Modality.ApplicationModal)
    dialogPane().content = content
  }

}

private class CountStatsProgressMonitor(contentCtrl: CountStatsPanelController) extends ProgressMonitor2[DirStats] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[DirStats]): Unit = {
    state.foreach(s => contentCtrl.updateStats(s, message))
  }

  override def notifyProgress(progress: Float, message: Option[String], state: Option[DirStats]): Unit = {
    state.foreach(s => contentCtrl.updateStats(s, message))
  }

  override def notifyFinished(message: String, state: Option[DirStats]): Unit = {
    println(s"Finished: $message, stats=$state")
    //        contentCtrl.showButtons(true, )
    contentCtrl.updateButtons(enableClose = true, enableCancel = false, enableSkip = false)
  }

  override def notifyError(message: String, state: Option[DirStats]): Unit = {
    state match {
      case Some(stats) => contentCtrl.updateStats(stats, Some(message))
      case _ => contentCtrl.updateMsg(message)
    }
    //        contentCtrl.showButtons(showClose = true, showCancel = false, showSkip = false)
    contentCtrl.updateButtons(enableClose = true, enableCancel = false, enableSkip = false)
  }
}