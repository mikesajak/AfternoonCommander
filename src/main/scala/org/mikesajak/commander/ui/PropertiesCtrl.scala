package org.mikesajak.commander.ui

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.{PathToParent, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.MyScalaFxImplicits._
import org.mikesajak.commander.ui.controller.PropertiesPanelController
import scalafx.Includes._
import scalafx.scene.control.ButtonType

class PropertiesCtrl(statusMgr: StatusMgr, appController: ApplicationController) {
  def handlePropertiesAction(): Unit = {
    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val targetPaths = selectedTab.controller.selectedPaths
                                 .filter(p => !p.isInstanceOf[PathToParent])

    if (targetPaths.nonEmpty) {
      showPropertiesOf(targetPaths.head)
    }
  }

  def showPropertiesOf(path: VPath): Unit = {
    val settingsLayout = "/layout/properties-panel.fxml"

    val (contentPane, ctrl) = UILoader.loadScene[PropertiesPanelController](settingsLayout)
    val dialog = UIUtils.mkModalDialog[ButtonType](appController.mainStage, contentPane)

    dialog.title = s"Properties of ${path.name}"
    dialog.dialogPane.value.buttonTypes = Seq(ButtonType.Close)

    ctrl.init(path)

    dialog.setWindowSize(800, 400)
    dialog.show()
  }
}
