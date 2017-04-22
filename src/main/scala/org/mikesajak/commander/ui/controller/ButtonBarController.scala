package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.ApplicationController

import scalafx.Includes.handle
import scalafx.scene.control.Button
import scalafxml.core.macros.sfxml

/**
  * Created by mike on 09.04.17.
  */
@sfxml
class ButtonBarController(viewButton: Button,
                          editButton: Button,
                          copyButton: Button,
                          moveButton: Button,
                          mkdirButton: Button,
                          deleteButton: Button,
                          exitButton: Button,
                          applicationController: ApplicationController) {
    viewButton.disable = true
    editButton.disable = true
    copyButton.disable = true
    moveButton.disable = true
    mkdirButton.disable = true
    deleteButton.disable = true
    exitButton.onAction = handle {
      applicationController.exitApplication()
    }
}
