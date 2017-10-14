package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.{ApplicationController, OperationMgr}

import scalafxml.core.macros.sfxml

/**
  * Created by mike on 22.04.17.
  */
@sfxml
class MenuController(appController: ApplicationController,
                     opsManager: OperationMgr) {

  def onFindAction(): Unit = opsManager.handleFindAction()

  def onSettingsAction(): Unit = opsManager.handleSettingsAction()
}
