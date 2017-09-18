package org.mikesajak.commander.ui.controller.ops

import javafx.beans.value.{ChangeListener, ObservableValue}

import org.mikesajak.commander.ui.ResourceManager

import scalafx.application.Platform
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

trait MkDirPanelController {
  def parentFolderLabel: Label
  def folderNameCombo: ComboBox[String]

  def init(parentFolderName: String, parentDialog: Dialog[String]): Unit
}

@sfxml
class MkDirPanelControllerImpl(headerImageView: ImageView,
                               val parentFolderLabel: Label,
                               val folderNameCombo: ComboBox[String],
                               okButton: Button,
                               cancelButton: Button,

                               resourceMgr: ResourceManager)
    extends MkDirPanelController {

  override def init(parentFolderName: String, parentDialog: Dialog[String]): Unit = {
    parentFolderLabel.text = parentFolderName
    folderNameCombo.promptText = "Raz dwa trzy"

    // todo: fill combo suggestion list

//    folderNameCombo.promptText = "New folder name" // todo: previous folder (top of combo suggesnion list)

//    folderNameCombo.editor.value.setOnKeyTyped( keyEvent => {
//      folder
//      println(s"MkDirPanelController: okButton disabled = ${okButton.disabled}")
//    })

    headerImageView.image = resourceMgr.getIcon("folder-plus-48.png")

    folderNameCombo.editor.value.textProperty().addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        val folderName = newValue
        okButton.disable = folderName == null || folderName.length == 0
      }
    })

    okButton.disable = true

    okButton.onAction = e => {
      parentDialog.close()
      parentDialog.result = folderNameCombo.value.value
    }

    cancelButton.onAction = e => {
      parentDialog.dialogPane().getButtonTypes.addAll(ButtonType.Cancel)
      parentDialog.close()
      parentDialog.result = null
    }
    
    Platform.runLater {
      folderNameCombo.requestFocus()
    }
  }
}
