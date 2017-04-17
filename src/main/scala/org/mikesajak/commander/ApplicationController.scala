package org.mikesajak.commander

import com.google.inject.Inject
import org.mikesajak.commander.config.Configuration

import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.Stage

/**
  * Created by mike on 09.04.17.
  */
class ApplicationController @Inject()(config: Configuration) {

  private var mainStage0: Option[Stage] = None

  def mainStage: Stage = mainStage0.getOrElse(throw new IllegalStateException("UI window/stage is not initialized yet"))
  // TODO: probably not the best way to do it...
  def mainStage_=(stage: Stage): Unit = {
    if (mainStage0.isDefined)
      throw new IllegalStateException("UI window/stage is already initialized")
    mainStage0 = Some(stage)
  }

  def exitApplication(): Unit = {
    val confirm = config.boolProperty("application.exitConfirmation").getOrElse(true)

    val exit = if (confirm) askUserForExit()
    else true

    if (exit) {
      // TODO: save config, close connections, etc.
      config.intProperty("window.width", mainStage.width.toInt)
      config.intProperty("window.height", mainStage.height.toInt)

      Platform.exit()
    }
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
