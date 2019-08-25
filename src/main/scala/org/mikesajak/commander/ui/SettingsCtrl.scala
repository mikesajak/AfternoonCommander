package org.mikesajak.commander.ui

import org.mikesajak.commander.ApplicationController
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.stage.{Modality, Stage, StageStyle}

class SettingsCtrl(appController: ApplicationController) {
  def handleSettingsAction(): Unit = {
    val settingsLayout = "/layout/settings-panel-layout.fxml"

    val (root, _) = UILoader.loadScene(settingsLayout)
    val stage = new Stage()
    stage.initModality(Modality.ApplicationModal)
    stage.initStyle(StageStyle.Utility)
    stage.initOwner(appController.mainStage)
    stage.scene = new Scene(root, 500, 400)
    stage.show()
  }
}
