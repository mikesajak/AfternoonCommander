package org.mikesajak.commander

import javafx.{scene => jfxs}

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.UILoader

import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.Scene


/**
 * Created by mike on 25.10.14.
 */
object Main extends JFXApp {
  private val logger = Logger("Main")
  private val mainPanelDef: String = "/layout/main-panel-layout.fxml"

  logger.info(s"AfternoonCommander starting")

  val root = UILoader.loadScene(mainPanelDef)

  stage = new PrimaryStage() {
    title = "AfternoonCommander"
    scene = new Scene(root)
  }

  val injector = ApplicationContext.globalInjector.createChildInjector()
  val config = injector.getInstance(classOf[Configuration])
  val appController= injector.getInstance(classOf[ApplicationController])

  Platform.implicitExit = false
  stage.onCloseRequest = we => if (!appController.exitApplication()) we.consume()

  appController.mainStage = stage

  stage.width = config.intProperty("window", "width").getOrElse(1000): Int
  stage.height = config.intProperty("window", "height").getOrElse(600): Int


  override def main(args: Array[String]): Unit = {
    super.main(args)
  }
}

