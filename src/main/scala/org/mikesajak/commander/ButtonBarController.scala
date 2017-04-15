package org.mikesajak.commander

import com.google.inject.Inject

import scalafx.Includes.handle
import scalafx.scene.control.Button

/**
  * Created by mike on 09.04.17.
  */
class ButtonBarController @Inject()(applicationController: ApplicationController) {
  def configureButtons(view: Button, edit: Button, copy: Button, move: Button,
                       mkdir: Button, delete: Button, exit: Button) = {
    view.disable = true
    edit.disable = true
    copy.disable = true
    move.disable = true
    mkdir.disable = true
    delete.disable = true
    exit.onAction = handle {
      applicationController.exitApplication()
    }
  }
}
