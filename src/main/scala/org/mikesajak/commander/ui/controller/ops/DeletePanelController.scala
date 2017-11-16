package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.ResourceManager

import scala.util.{Failure, Success, Try}
import scalafx.Includes._
import scalafx.scene.control.{ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

trait DeletePanelController {
  def init(targetPath: VPath, dirStats: Try[Option[DirStats]], dialog: Dialog[ButtonType]): Unit
}

@sfxml
class DeletePanelControllerImpl(pathTypeLabel: Label,
                                pathToTargetLabel: Label,
                                targetNameLabel: Label,
                                statsPanel: Pane,
                                dirStatsPanel: Pane,
                                @nested[DirStatsPanelControllerImpl] dirStatsPanelController: DirStatsPanelController,

                                fileStatsPanel: Pane,
                                @nested[FileStatsPanelControllerImpl] fileStatsPanelController: FileStatsPanelController,

                                statsMessageLabel: Label,
                                resourceMgr: ResourceManager) extends DeletePanelController {

  def init(targetPath: VPath, dirStats: Try[Option[DirStats]], dialog: Dialog[ButtonType]): Unit = {
    val targetPathType = if (targetPath.isDirectory) "directory" else "file"

    dialog.title = s"Afternoon Commander - delete $targetPathType"

    dialog.headerText = s"Delete selected $targetPathType?"
    dialog.graphic = new ImageView(resourceMgr.getIcon("delete-circle-48.png"))

    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Yes, ButtonType.No)

    pathTypeLabel.text = startWithUpper(targetPathType)

    val targetParentName = targetPath.parent.map(_.absolutePath).getOrElse("")
    pathToTargetLabel.text = s"$targetParentName/"

    targetNameLabel.text = targetPath.name

    val icon = if (targetPath.isDirectory) "folder-24.png" else "file-24.png"
    pathToTargetLabel.graphic = new ImageView(resourceMgr.getIcon(icon))

    // create bindings - to resize parent layout on disable/hide
    statsPanel.managed <== statsPanel.visible
    dirStatsPanel.managed <== dirStatsPanel.visible
    fileStatsPanel.managed <== fileStatsPanel.visible
    statsMessageLabel.managed <== statsMessageLabel.visible

    if (targetPath.isDirectory) {
      dirStats match {
        case Success(Some(stats)) =>
          dirStatsPanel.visible = true
          fileStatsPanel.visible = false
          statsMessageLabel.visible = false
          dirStatsPanelController.init(targetPath, Some(stats))

        case Failure(reason) =>
          statsMessageLabel.visible = true
          statsMessageLabel.text = s"Couldn't get directory statistics because of: $reason"
          dirStatsPanel.visible = false
          fileStatsPanel.visible = false

        case Success(None) =>
          statsMessageLabel.visible = true
          statsMessageLabel.text = s"Statistics count skipped. Operation progress won't be available."
          dirStatsPanel.visible = false
          fileStatsPanel.visible = false
      }
    } else {
        dirStatsPanel.visible = false
        statsMessageLabel.visible = false
        fileStatsPanel.visible = true

        fileStatsPanelController.init(targetPath)
    }
  }

  private def startWithUpper(text: String) = text.length match {
    case 0 => text
    case 1 => text.toUpperCase
    case _ => s"${text(0).toString.toUpperCase}${text.substring(1)}"
  }
}
