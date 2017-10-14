package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.ui.ResourceManager

import scalafx.Includes._
import scalafx.scene.control.{ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

trait DeletePanelController {
  def init(targetPath: VPath, dialog: Dialog[ButtonType])
}

@sfxml
class DeletePanelControllerImpl(pathTypeLabel: Label,
                                pathToTargetLabel: Label,
                                targetNameLabel: Label,
                                resourceMgr: ResourceManager) extends DeletePanelController {

  def init(targetPath: VPath, dialog: Dialog[ButtonType]): Unit = {
    val targetPathType = if (targetPath.isDirectory) "directory" else "file"

    dialog.title = s"Afternoon Commander - delete $targetPathType"
    dialog.headerText = s"Delete selected $targetPathType?"
    dialog.graphic = new ImageView(resourceMgr.getIcon("delete-circle-48.png"))
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Yes, ButtonType.No)

    pathTypeLabel.text = startWithUpper(targetPathType)

    val targetParentName = targetPath.parent.map(_.absolutePath).getOrElse("")
    pathToTargetLabel.text = s"$targetParentName/"

    val icon = if (targetPath.isDirectory) "folder-24.png" else "file-24.png"
//    targetNameLabel
    pathToTargetLabel.graphic = new ImageView(resourceMgr.getIcon(icon))

    targetNameLabel.text = targetPath.name
  }

  private def startWithUpper(text: String) = text.length match {
    case 0 => text
    case 1 => text.toUpperCase
    case _ => s"${text(0).toString.toUpperCase}${text.substring(1)}"
  }
}
