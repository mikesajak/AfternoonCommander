package org.mikesajak.commander.ui.controller.ops

import javafx.beans.value.{ChangeListener, ObservableValue}

import scalafx.application.Platform
import scalafx.scene.control.{Button, ComboBox, Label}
import scalafxml.core.macros.sfxml

trait MkDirPanelController {
  def parentFolderLabel: Label
  def folderNameCombo: ComboBox[String]

  def init(parentFolderName: String, okButton: Button): Unit
}

@sfxml
class MkDirPanelControllerImpl(val parentFolderLabel: Label,
                           val folderNameCombo: ComboBox[String]) extends MkDirPanelController {

  override def init(parentFolderName: String, okButton: Button): Unit = {
    parentFolderLabel.text = parentFolderName

    // todo: fill combo suggestion list

    folderNameCombo.promptText = "New folder name" // todo: previous folder (top of combo suggesnion list)

//    folderNameCombo.editor.value.setOnKeyTyped( keyEvent => {
//      folder
//      println(s"MkDirPanelController: okButton disabled = ${okButton.disabled}")
//    })

    folderNameCombo.editor.value.textProperty().addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        val folderName = newValue
        okButton.disable = folderName == null || folderName.length == 0
      }
    })

    okButton.disable = true
    
    Platform.runLater {
      folderNameCombo.requestFocus()
    }
  }
}
