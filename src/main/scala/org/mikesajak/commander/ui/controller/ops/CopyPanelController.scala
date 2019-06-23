package org.mikesajak.commander.ui.controller.ops

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{VDirectory, VPath}
import org.mikesajak.commander.task.{BackgroundService, DirStats, DirStatsTask}
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import org.mikesajak.commander.util.PathUtils
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.concurrent.Service
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

trait CopyPanelController {
  def init(sourcePaths: Seq[VPath], targetDir: VDirectory, dialog: Dialog[ButtonType]): Service[DirStats]
}

@sfxml
class CopyPanelControllerImpl(sourcePathTypeLabel: Label,
                              pathToSourceLabel: Label,
                              sourceNameLabel: Label,
                              sourcePathsListView: ListView[String],
                              statsPanel: Pane,
                              @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,
                              targetDirCombo: ComboBox[String],
                              summaryMessageLabel: Label,

                              resourceMgr: ResourceManager) extends CopyPanelController {

  private val logger = Logger[CopyPanelControllerImpl]

  logger.debug(s"CopyPanelControllerImpl - constr")

  override def init(sourcePaths: Seq[VPath], targetDir: VDirectory, dialog: Dialog[ButtonType]): Service[DirStats] = {
    val pathType = pathTypeOf(sourcePaths)
    dialog.title = s"${resourceMgr.getMessage("app.name")} - ${resourceMgr.getMessage(s"copy_dialog.title")}"
    dialog.headerText = resourceMgr.getMessage(s"copy_dialog.header.${pathType.name}")
    dialog.graphic = new ImageView(resourceMgr.getIcon("content-copy-black.png", IconSize.Big))
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Yes, ButtonType.No)
    sourcePathTypeLabel.text = resourceMgr.getMessage(s"copy_dialog.to_copy.${pathType.name}")

    targetDirCombo.selectionModel.value.select(targetDir.toString)

    // create bindings - to resize parent layout on disable/hide
    sourcePathsListView.managed <== sourcePathsListView.visible
    statsPanel.managed <== statsPanel.visible

    val targetPath = sourcePaths.head
    val targetParentName = targetPath.parent.map(_.absolutePath).getOrElse("")
    pathToSourceLabel.text = s"${PathUtils.shortenPathTo(targetParentName, 80)}/"
    pathToSourceLabel.tooltip = s"$targetParentName/"
    pathToSourceLabel.graphic = new ImageView(resourceMgr.getIcon(pathType.icon, IconSize.Small))

    if (pathType != MultiPaths) {
      sourceNameLabel.text = targetPath.name
      sourcePathsListView.visible = false
    } else {
      sourceNameLabel.text = "[" + resourceMgr.getMessageWithArgs("copy_dialog.num_elements", Array(sourcePaths.size)) + "]"
      sourcePathsListView.visible = true
      sourcePathsListView.items = ObservableBuffer(sourcePaths.map(p => if (p.isDirectory) s"${p.name}/" else p.name))
    }

    summaryMessageLabel.text = resourceMgr.getMessage("copy_dialog.progress_not_available.label")
    summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("comment-alert-outline.png", IconSize.Small))
    summaryMessageLabel.tooltip = resourceMgr.getMessage("copy_dialog.progress_not_available.tooltip")

    statsPanelController.init(sourcePaths)

    val statsService = new BackgroundService(new DirStatsTask(sourcePaths))
    statsService.onRunning = e => statsPanelController.notifyStarted()
    statsService.onFailed = e => notifyError(Option(statsService.value.value), statsService.message.value)
    statsService.onSucceeded = e => notifyFinished(statsService.value.value, None)
    statsService.value.onChange { (_, _, stats) => statsPanelController.updateStats(stats, None)}

    dialog.onShown = e => statsService.start()

    statsService
  }

  private def pathTypeOf(targetPaths: Seq[VPath]): PathType =
    targetPaths match {
      case p if p.size == 1 && p.head.isDirectory => SingleDir
      case p if p.size == 1 => SingleFile
      case p => MultiPaths
    }

  private def notifyFinished(stats: DirStats, message: Option[String]): Unit = {
    statsPanelController.notifyFinished(stats, message)

    summaryMessageLabel.text = resourceMgr.getMessage("copy_dialog.progress_available.label")
    summaryMessageLabel.graphic = null
    summaryMessageLabel.tooltip = resourceMgr.getMessage("copy_dialog.progress_available.tooltip")
  }

  private def notifyError(stats: Option[DirStats], message: String): Unit = {
    statsPanelController.notifyError(stats, message)

    summaryMessageLabel.text = message
    summaryMessageLabel.tooltip = "An error occurred while processing IO operation."
    summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("alert-circle.png", IconSize.Small))
  }
}

