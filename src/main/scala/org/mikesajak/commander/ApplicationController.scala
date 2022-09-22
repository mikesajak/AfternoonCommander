package org.mikesajak.commander

import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import org.mikesajak.commander.util.Check
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.Stage

/**
  * Created by mike on 09.04.17.
  */
class ApplicationController(config: Configuration) {

  private var mainStage0: Stage =_
  private var application0: JFXApp3 = _

  // TODO: probably not the best way to do it...
  def init(stage: Stage, app: JFXApp3): Unit = {
    Check.state(mainStage0 == null && application0 == null, "UI window/stage is already initialized")
    mainStage0 = stage
    application0 = app
  }

  def mainStage: Stage = {
    Check.state(mainStage0 != null, "UI window/stage is not initialized yet")
    mainStage0
  }

  def application: JFXApp3 = {
    Check.state(application0 != null, "Application has not been defined")
    application0
  }

  def exitApplication(): Unit = exitApplication(() => false)
  def exitApplication(exitAction: () => Boolean): Unit = {
    if (canExit) {
      // TODO: save config, close connections, etc.
      config.intProperty(ConfigKeys.WindowWidth) := mainStage.width.toInt
      config.intProperty(ConfigKeys.WindowHeight) := mainStage.height.toInt
      config.save()

      if (!exitAction())
        Platform.exit()
    }
  }

  private def canExit: Boolean = {
    val confirm = config.boolProperty(ConfigKeys.ExitConfirmation).getOrElse(true)

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
  val configPath = s"${System.getProperty("user.dir")}" // fixme: for debug purposes, change this to dir in user home, e.g. .afternooncommander/ or something
  val configFile = "afternooncommander.conf"
}