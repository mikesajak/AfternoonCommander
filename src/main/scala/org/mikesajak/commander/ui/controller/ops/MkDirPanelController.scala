package org.mikesajak.commander.ui.controller.ops

import javafx.beans.value.{ChangeListener, ObservableValue}
import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.ui.{IconSize, ResourceManager, UIUtils}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

trait MkDirPanelController {
  def folderNameCombo: ComboBox[String]

  def init(parentFolder: VDirectory, parentDialog: Dialog[String]): Unit
}

@sfxml
class MkDirPanelControllerImpl(headerImageView: ImageView,
                               val parentFolderPathLabel: Label,
                               val parentFolderNameLabel: Label,
                               val folderNameCombo: ComboBox[String],
                               resourceMgr: ResourceManager) extends MkDirPanelController {

  override def init(parentFolder: VDirectory, dialog: Dialog[String]): Unit = {
    val path = parentFolder.parent.map(_.absolutePath).getOrElse("")
    parentFolderPathLabel.text = path + "/"
    parentFolderNameLabel.text = parentFolder.name

    dialog.headerText = resourceMgr.getMessage("mkdir_dialog.header")
    dialog.graphic = new ImageView(resourceMgr.getIcon("folder-plus.png", IconSize.Big))


    dialog.title =resourceMgr.getMessage("mkdir_dialog.title")
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    // todo: fill combo suggestion list

//    folderNameCombo.promptText = "New folder name" // todo: previous folder (top of combo suggesnion list)

//    folderNameCombo.editor.value.setOnKeyTyped( keyEvent => {
//      folder
//      println(s"MkDirPanelController: okButton disabled = ${okButton.disabled}")
//    })

//    headerImageView.image = resourceMgr.getIcon("folder-plus-48.png")

    val okButton: Button = UIUtils.dialogButton(dialog, ButtonType.OK)
    val cancelButton = UIUtils.dialogButton(dialog, ButtonType.Cancel)

    folderNameCombo.editor.value.textProperty().addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        val folderName = newValue
        okButton.disable = folderName == null || folderName.length == 0
      }
    })

    okButton.disable = true

    dialog.resultConverter =  {
      case ButtonType.OK => folderNameCombo.value.value
      case _ => null
    }

//    okButton.onAction = e => {
//      parentDialog.close()
//      parentDialog.result = folderNameCombo.value.value
//    }
//
//    cancelButton.onAction = e => {
//      parentDialog.dialogPane().getButtonTypes.addAll(ButtonType.Cancel)
//      parentDialog.close()
//      parentDialog.result = null
//    }
    
    Platform.runLater {
      folderNameCombo.requestFocus()
    }
  }
}
