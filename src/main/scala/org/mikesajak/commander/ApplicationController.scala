package org.mikesajak.commander

import com.google.inject.Inject
import org.mikesajak.commander.config.Configuration

import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

/**
  * Created by mike on 09.04.17.
  */
class ApplicationController @Inject()(config: Configuration) {
  def exitApplication() = {
    val confirm = config.getBoolSetting("application.exitConfirmation").getOrElse(true)

    val exit = if (confirm) askUserForExit()
    else true

    if (exit) {
      // TODO: save config, close connections, etc.
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
