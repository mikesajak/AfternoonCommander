package org.mikesajak.commander.status

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.ui.{ResourceManager, UILoader}

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control.Dialog
import scalafx.scene.image.ImageView
import scalafx.stage.{Modality, Stage, StageStyle}

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

    val root = UILoader.loadScene(settingsLayout)
//    val stage = new Stage()

    val dialog = new Dialog[Option[String]]() {
      title ="Afternoon Commander - create folder"
      headerText = "Create new folder"
      graphic = new ImageView(resourceMgr.getIcon("folder-plus-48.png"))

      initOwner(appController.mainStage)
      initStyle(StageStyle.Utility)
      initModality(Modality.ApplicationModal)

      dialogPane().content = root

      val okButton = dialogPane().lookup("#okButton")
      val cancelButton = dialogPane().lookup("#cancelButton")

      resultConverter = x => {
        println(s"x=$x")
        None
      }

      width = 500
      height = 250

      dialogPane()
    }

    val result = dialog.showAndWait()

    println(s"MkDir dialog result=$result")
  }

  def handleDelete(): Unit = {
    logger.warn(s"handleDelete - Not implemented yet!")
  }

  def handleExit(): Unit = {
    appController.exitApplication()
  }

}
