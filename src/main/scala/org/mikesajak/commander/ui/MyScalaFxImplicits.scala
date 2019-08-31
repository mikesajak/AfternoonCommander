package org.mikesajak.commander.ui

import javafx.stage
import scalafx.scene.control.Dialog

object MyScalaFxImplicits {
//  implicit def jfxButton2SfxButton(button: jfxctrl.Button): Button = new Button(button)

  implicit class RichDialog[A](val self: Dialog[A]) {
    def setWindowSize(width: Int, height: Int): Unit = {
      val window = self.getDialogPane.getScene.getWindow.asInstanceOf[stage.Stage]
      window.setMinWidth(width)
      window.setMinHeight(height)
    }
  }
}
