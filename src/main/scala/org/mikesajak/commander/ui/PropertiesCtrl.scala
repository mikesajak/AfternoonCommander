package org.mikesajak.commander.ui

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.{PathToParent, VPath}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.MyScalaFxImplicits._
import org.mikesajak.commander.ui.controller.PropertiesPanelController
import scalafx.Includes._
import scalafx.scene.control.ButtonType
import scribe.Logging

import scala.concurrent.CancellationException

class PropertiesCtrl(statusMgr: StatusMgr, appController: ApplicationController) extends Logging {

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

    val backgroundServices = ctrl.init(path)

    dialog.onShown = _ => backgroundServices.foreach(_.start())
    dialog.setWindowSize(700, 600)
    dialog.showAndWait()

    backgroundServices.foreach { bgService =>
      try {
        bgService.cancel()
      } catch {
        case _: CancellationException => logger.debug(s"Background service cancelled")
      }
    }
  }
}
