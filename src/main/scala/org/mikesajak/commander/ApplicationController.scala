package org.mikesajak.commander

import org.mikesajak.commander.config.Configuration

import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.Stage

/**
  * Created by mike on 09.04.17.
  */
class ApplicationController(config: Configuration) {

  private var mainStage0: Option[Stage] = None

  def mainStage: Stage = {
    if (mainStage0.isDefined) mainStage0.get
    else throw new IllegalStateException("UI window/stage is not initialized yet")
  }
  // TODO: probably not the best way to do it...
  def mainStage_=(stage: Stage): Unit = {
    if (mainStage0.isDefined)
      throw new IllegalStateException("UI window/stage is already initialized")
    mainStage0 = Some(stage)
  }

  def exitApplication(): Boolean = {
    if (canExit) {
      // TODO: save config, close connections, etc.
      config.setProperty("window", "width", mainStage.width.toInt)
      config.setProperty("window", "height", mainStage.height.toInt)
      config.save()

      Platform.exit()
    }
    false
  }

  private def canExit: Boolean = {
    val confirm = config.boolProperty("application", "exitConfirmation").getOrElse(true)

    if (confirm) askUserForExit()
    else true
  }

  private def askUserForExit(): Boolean = {
    val alert = new Alert(AlertType.Confirmation) {
      //        initOwner(stage)
      title = "Confirm application exit"
      headerText = "You're about to quit application."
      contentText = "Are you sure?"
      buttonTypes = Seq(ButtonType.No, ButtonType.Yes)
    }

    alert.showAndWait() match {
      case Some(ButtonType.Yes) => true
      case _ => false
    }
  }

}

object ApplicationController {
  val configPath = s"${System.getProperty("user.dir")}/.." // fixme: for debug purposes, change this to dir in user home, e.g. .afternooncommander/ or something
  val configFile = "afternooncommander.conf"
}