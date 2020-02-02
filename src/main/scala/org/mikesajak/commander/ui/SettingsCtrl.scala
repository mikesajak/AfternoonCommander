package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.ui.controller.settings.SettingsPanelController
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.stage.{Modality, Stage, StageStyle}

class SettingsCtrl(appController: ApplicationController) {
  private val logger = Logger[SettingsCtrl]

  def handleSettingsAction(): Unit = {
    val settingsLayout = "/layout/settings-panel-layout.fxml"

    val (root, ctrl) = UILoader.loadScene[SettingsPanelController](settingsLayout)
    ctrl.init()

    val stage = new Stage()
    stage.initModality(Modality.ApplicationModal)
    stage.initStyle(StageStyle.Utility)
    stage.initOwner(appController.mainStage)
    stage.scene = new Scene(root, 500, 400)
    stage.show()
  }
}
