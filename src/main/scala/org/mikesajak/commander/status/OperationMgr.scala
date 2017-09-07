package org.mikesajak.commander.status

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.ui.{ResourceManager, UILoader}

import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.stage.{Modality, StageStyle}

class OperationMgr(statusMgr: StatusMgr,
                   resourceMgr: ResourceManager,
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
    dialog.dialogPane().content = UILoader.loadScene(settingsLayout)

    val result = dialog.showAndWait()

    println(s"MkDir dialog result=$result")
  }

  private def prepareOkCancelDialog() = {
    new Dialog[String]() {
      title ="Afternoon Commander"
//      headerText = "Create new folder"
//      graphic = new ImageView(resourceMgr.getIcon("folder-plus-48.png"))

      initOwner(appController.mainStage)
      initStyle(StageStyle.Utility)
      initModality(Modality.ApplicationModal)

      dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

//      resultConverter = bt => bt match {
//        case ButtonType.OK => "OK"
//        case ButtonType.Cancel => "Cancel"
//      }
//
//      dialogPane()
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

  def handleExit(): Unit = {
    appController.exitApplication()
  }

}
