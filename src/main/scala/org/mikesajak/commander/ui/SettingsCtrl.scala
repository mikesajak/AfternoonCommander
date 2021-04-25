package org.mikesajak.commander.ui

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.ui.MyScalaFxImplicits._
import org.mikesajak.commander.ui.controller.settings.SettingsPanelController
import scalafx.Includes._
import scribe.Logging

class SettingsCtrl(appController: ApplicationController) extends Logging{

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
