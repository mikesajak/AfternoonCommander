package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.ui.UILoader

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.stage.{Modality, Stage, StageStyle}
import scalafxml.core.macros.sfxml

/**
  * Created by mike on 22.04.17.
  */
@sfxml
class MenuController(appController: ApplicationController) {
  val settingsLayout = "/layout/settings-panel-layout.fxml"

  def onSettingsAction() {
    val root = UILoader.loadScene(settingsLayout)
    val stage = new Stage()
    stage.initModality(Modality.ApplicationModal)
    stage.initStyle(StageStyle.Utility)
    stage.initOwner(appController.mainStage)
    stage.scene = new Scene(root, 500, 400)
    stage.show()
  }
}
