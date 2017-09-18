package org.mikesajak.commander

import javafx.scene.control

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{FilesystemsManager, PathToParent, VDirectory}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.{ConsoleProgressMonitor, DirStatsTask, TestTask}
import org.mikesajak.commander.ui.controller.ops.MkDirPanelController
import org.mikesajak.commander.ui.{ResourceManager, UILoader}

import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.stage.{Modality, StageStyle}

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
    logger.warn(s"handleMkDir - Not implemented yet!")
    val curTab = statusMgr.selectedTabManager.selectedTab
    logger.debug(s"handleMkDir - curTab=$curTab")

    val settingsLayout = "/layout/ops/mkdir-dialog.fxml"

    val dialog = prepareOkCancelDialog()
    dialog.headerText = "Create new folder"
    dialog.graphic = new ImageView(resourceMgr.getIcon("folder-plus-48.png"))

    val (contentPane, contentCtrl) = UILoader.loadScene[MkDirPanelController](settingsLayout)
    val selectedTab = statusMgr.selectedTabManager.selectedTab

    val okButton = new Button(dialog.dialogPane().lookupButton(ButtonType.OK).asInstanceOf[control.Button])
    contentCtrl.init(selectedTab.dir.toString, okButton)
    dialog.dialogPane().content = contentPane

    val result = dialog.showAndWait()

    println(s"MkDir dialog result=$result")

    result.foreach { selButton =>
      if (selButton == ButtonType.OK) {
        val fs = selectedTab.dir.fileSystem
        val newPath = selectedTab.dir.mkChildDir(contentCtrl.folderNameCombo.value.value)
        fs.create(newPath)
      }
    }
  }

  private def prepareOkCancelDialog() = {
    new Dialog[String]() {
      title ="Afternoon Commander"
      initOwner(appController.mainStage)
      initStyle(StageStyle.Utility)
      initModality(Modality.ApplicationModal)
      dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
    }
  }

  def handleDelete(): Unit = {
    logger.warn(s"handleDelete - Not implemented yet!")
    val target = "file/directory" // todo: choose basing on actual selection in current panel

    val result =
      new Alert(AlertType.Warning) {
        title = "Afternoon Commander"
        headerText = s"Delete $target"
    //      graphic =
        contentText = s"Do you really want to delete selected $target"
        buttonTypes = Seq(ButtonType.Yes, ButtonType.No)

      }.showAndWait()

    println(s"Delete confirmation dialog result=$result")
  }

  def handleCountDirStats(): Unit = {
    val selectedRow = statusMgr.selectedTabManager.selectedTab.controller.selectedRow
    if (selectedRow != null) {
      val selectedPath = selectedRow.path
      if (selectedPath.isDirectory && !selectedPath.isInstanceOf[PathToParent]) {
          val selDir = selectedPath.asInstanceOf[VDirectory]
          taskManager.runTaskAsync(new DirStatsTask(selDir), new ConsoleProgressMonitor)
        } else {
          println(s"Cannot run count dir stats on file: $selectedPath")
        }
     } else println(s"No directory is selected")
  }

  def handleTestTask(sync: Boolean): Unit = {
    if (sync) taskManager.runTaskSync(new TestTask(), new ConsoleProgressMonitor)
    else      taskManager.runTaskAsync(new TestTask(), new ConsoleProgressMonitor)
  }

  def handleExit(): Unit = {
    appController.exitApplication()
  }

}
