package org.mikesajak.commander

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{FilesystemsManager, PathToParent, VDirectory}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.ops.{CountStatsPanelController, MkDirPanelController}
import org.mikesajak.commander.ui.{ResourceManager, UILoader}

import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
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

    val contentLayout = "/layout/ops/mkdir-dialog2.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[MkDirPanelController](contentLayout)
    val selectedTab = statusMgr.selectedTabManager.selectedTab

    val dialog = new Dialog[String]() {
      title ="Afternoon Commander"
      initOwner(appController.mainStage)
      initStyle(StageStyle.Utility)
      initModality(Modality.ApplicationModal)
      dialogPane().content = contentPane
    }

    contentCtrl.init(selectedTab.dir.toString, dialog)

    val result = dialog.showAndWait()

    println(s"MkDir dialog result=$result")

//    for (newDirName <- result) {
//      selectedTab.dir.mkChildDir(newDirName)
//      selectedTab.controller.reload()
//    }
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
    val contentLayout = "/layout/ops/count-stats-dialog.fxml"

    val (contentPane, contentCtrl) = UILoader.loadScene[CountStatsPanelController](contentLayout)
    val selectedTab = statusMgr.selectedTabManager.selectedTab

    val dialog = new Dialog[ButtonType]() {
      title = "Afternoon Commander"
      initOwner(appController.mainStage)
      initStyle(StageStyle.Utility)
      initModality(Modality.ApplicationModal)
      dialogPane().content = contentPane
    }

    contentCtrl.init(selectedTab.dir, dialog, showClose = true, showCancel = true, showSkip = false)
    contentCtrl.updateButtons(enableClose = false, enableCancel = true, enableSkip = false)

    class CountStatsProgressMonitor extends ProgressMonitor2[DirStats] {
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

    Option(statusMgr.selectedTabManager.selectedTab.controller.selectedRow)
      .map(_.path) match {
        case Some(selectedPath) =>
          if (selectedPath.isDirectory && !selectedPath.isInstanceOf[PathToParent]) {
            val selDir = selectedPath.asInstanceOf[VDirectory]
//            taskManager.runTaskAsync(new DirStatsTask(selDir), new ConsoleProgressMonitor2[DirStats])
            taskManager.runTaskAsync(new DirStatsTask(selDir), new CountStatsProgressMonitor)
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

}
