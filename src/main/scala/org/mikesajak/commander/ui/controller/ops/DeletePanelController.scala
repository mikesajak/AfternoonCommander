package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.{ResourceManager, StatsUpdateListener}

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.{ButtonType, Dialog, Label}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

trait DeletePanelController extends StatsUpdateListener {
  def init(targetPaths: Seq[VPath], stats: DirStats, dialog: Dialog[ButtonType]): Unit
}

@sfxml
class DeletePanelControllerImpl(pathTypeLabel: Label,
                                pathToTargetLabel: Label,
                                targetNameLabel: Label,
                                statsPanel: Pane,
                                @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,

                                statsMessageLabel: Label,
                                summaryMessageLabel: Label,

                                resourceMgr: ResourceManager) extends DeletePanelController {

  sealed abstract class PathType(val name: String, val icon: String)
  case object SingleFile extends PathType("file", "file-24.png")
  case object SingleDir extends PathType("directory", "folder-24.png")
  case object MultiPaths extends PathType("paths", "folder-multiple-24.png")


  def init(targetPaths: Seq[VPath], stats: DirStats, dialog: Dialog[ButtonType]): Unit = {
    val pathType = pathTypeOf(targetPaths)
    dialog.title = s"${resourceMgr.getMessage("app.name")} - ${resourceMgr.getMessage(s"delete_dialog.title")}"
    dialog.headerText = resourceMgr.getMessage(s"delete_dialog.header.${pathType.name}")
    dialog.graphic = new ImageView(resourceMgr.getIcon("delete-circle-48.png"))
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Yes, ButtonType.No)
    pathTypeLabel.text = resourceMgr.getMessage(s"delete_dialog.to_delete.${pathType.name}")

    if (pathType != MultiPaths) {
      val targetPath = targetPaths.head
      val targetParentName = targetPath.parent.map(_.absolutePath).getOrElse("")
      pathToTargetLabel.text = s"$targetParentName/"
      targetNameLabel.text = targetPath.name
    } else {
      pathToTargetLabel.text = ""
      targetNameLabel.text = resourceMgr.getMessageWithArgs("delete_dialog.num_elements", Array(targetPaths.size))
    }

    pathToTargetLabel.graphic = new ImageView(resourceMgr.getIcon(pathType.icon))

    // create bindings - to resize parent layout on disable/hide
    statsPanel.managed <== statsPanel.visible
    statsMessageLabel.managed <== statsMessageLabel.visible

    if (pathType == SingleFile) {
      statsMessageLabel.visible = false
    } else {
      statsMessageLabel.visible = false
      summaryMessageLabel.text = resourceMgr.getMessage("delete_dialog.progress_not_available.label")
      summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("comment-alert-outline-24.png"))
      summaryMessageLabel.tooltip = resourceMgr.getMessage("delete_dialog.progress_not_available.tooltip")
    }

    statsPanel.height.onChange { (_, oldVal, newVal) =>
      if (newVal.doubleValue > oldVal.doubleValue) dialog.dialogPane.value.getScene.getWindow.sizeToScene()
    }
    statsPanel.width.onChange { (_, oldVal, newVal) =>
      if (newVal.doubleValue > oldVal.doubleValue) dialog.dialogPane.value.getScene.getWindow.sizeToScene()
    }

    statsPanelController.init(targetPaths)
  }

  private def pathTypeOf(targetPaths: Seq[VPath]): PathType =
    targetPaths match {
      case p if p.size == 1 && p.head.isDirectory => SingleDir
      case p if p.size == 1 => SingleFile
      case p => MultiPaths
    }

  private def startWithUpper(text: String) = text.length match {
    case 0 => text
    case 1 => text.toUpperCase
    case _ => s"${text(0).toString.toUpperCase}${text.substring(1)}"
  }

  override def updateStats(stats: DirStats, message: Option[String]): Unit = {
    statsPanelController.updateStats(stats)
  }

  override def updateMessage(message: String): Unit = {
    println(s"TODO: message: $message")
  }

  override def notifyFinished(stats: DirStats, message: Option[String]): Unit = {
    statsPanelController.updateStats(stats)
    Platform.runLater {
      summaryMessageLabel.text = resourceMgr.getMessage("delete_dialog.progress_available.label")
      summaryMessageLabel.graphic = null
      summaryMessageLabel.tooltip = resourceMgr.getMessage("delete_dialog.progress_available.tooltip")
    }
  }

  override def notifyError(stats: Option[DirStats], message: String): Unit = {
    stats.foreach(st => statsPanelController.updateStats(st))
    Platform.runLater {
      summaryMessageLabel.text = message
      summaryMessageLabel.tooltip = "An error occurred while processing IO operation."
      summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("alert-circle-24.png"))
    }
  }
}
