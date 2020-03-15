package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.ui.MyScalaFxImplicits._
import org.mikesajak.commander.ui.controller.settings.SettingsPanelController
import scalafx.Includes._

class SettingsCtrl(appController: ApplicationController) {
  private val logger = Logger[SettingsCtrl]

  def handleSettingsAction(): Unit = {
    val settingsLayout = "/layout/settings-panel-layout.fxml"

    val (root, ctrl) = UILoader.loadScene[SettingsPanelController](settingsLayout)

    val dialog = UIUtils.mkModalDialogNoButtonOrder[Any](appController.mainStage, root)
    ctrl.init(dialog)

    dialog.setWindowSize(650, 450)
    dialog.setWindowMinSize(300, 300)
    dialog.resizable = true
    dialog.showAndWait()
  }
}
