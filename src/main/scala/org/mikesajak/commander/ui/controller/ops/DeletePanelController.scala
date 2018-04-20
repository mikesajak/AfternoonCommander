package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.{ResourceManager, StatsUpdateListener}
import org.mikesajak.commander.util.PathUtils

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ButtonType, Dialog, Label, ListView}
import scalafx.scene.image.ImageView
import scalafxml.core.macros.{nested, sfxml}

trait DeletePanelController extends StatsUpdateListener {
  def init(targetPaths: Seq[VPath], stats: DirStats, dialog: Dialog[ButtonType]): Unit
}

@sfxml
class DeletePanelControllerImpl(pathTypeLabel: Label,
                                pathToTargetLabel: Label,
                                targetNameLabel: Label,
                                pathsListView: ListView[String],
//                                statsPanel: Pane,
                                @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,

                                statsMessageLabel: Label,
                                summaryMessageLabel: Label,

                                resourceMgr: ResourceManager) extends DeletePanelController {

  override def init(targetPaths: Seq[VPath], stats: DirStats, dialog: Dialog[ButtonType]): Unit = {
    val pathType = pathTypeOf(targetPaths)
    dialog.title = s"${resourceMgr.getMessage("app.name")} - ${resourceMgr.getMessage(s"delete_dialog.title")}"
    dialog.headerText = resourceMgr.getMessage(s"delete_dialog.header.${pathType.name}")
    dialog.graphic = new ImageView(resourceMgr.getIcon("delete-circle-48.png"))
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Yes, ButtonType.No)
    pathTypeLabel.text = resourceMgr.getMessage(s"delete_dialog.to_delete.${pathType.name}")

    // create bindings - to resize parent layout on disable/hide
    pathsListView.managed <== pathsListView.visible
//    statsPanel.managed <== statsPanel.visible
    statsMessageLabel.managed <== statsMessageLabel.visible

    val targetPath = targetPaths.head
    val targetParentName = targetPath.parent.map(_.absolutePath).getOrElse("")
    pathToTargetLabel.text = s"${PathUtils.shortenPathTo(targetParentName, 80)}/"
    pathToTargetLabel.tooltip = s"$targetParentName/"
    pathToTargetLabel.graphic = new ImageView(resourceMgr.getIcon(pathType.icon))

    if (pathType != MultiPaths) {
      targetNameLabel.text = targetPath.name
      pathsListView.visible = false
    } else {
      targetNameLabel.text = "[" + resourceMgr.getMessageWithArgs("delete_dialog.num_elements", Array(targetPaths.size)) + "]"
      pathsListView.visible = true
      pathsListView.items = ObservableBuffer(targetPaths.map(p => if (p.isDirectory) s"${p.name}/" else p.name))
    }

    if (pathType == SingleFile) {
      statsMessageLabel.visible = false
    } else {
      statsMessageLabel.visible = true
      statsMessageLabel.graphic = new ImageView(resourceMgr.getIcon("loading-chasing-arrows.gif"))
      statsMessageLabel.text = resourceMgr.getMessage("delete_dialog.stats_counting.label")
      summaryMessageLabel.text = resourceMgr.getMessage("delete_dialog.progress_not_available.label")
      summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("comment-alert-outline-24.png"))
      summaryMessageLabel.tooltip = resourceMgr.getMessage("delete_dialog.progress_not_available.tooltip")
    }

    statsPanelController.init(targetPaths)
  }

  private def pathTypeOf(targetPaths: Seq[VPath]): PathType =
    targetPaths match {
      case p if p.size == 1 && p.head.isDirectory => SingleDir
      case p if p.size == 1 => SingleFile
      case p => MultiPaths
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
      statsMessageLabel.graphic = null
      statsMessageLabel.text = null

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
