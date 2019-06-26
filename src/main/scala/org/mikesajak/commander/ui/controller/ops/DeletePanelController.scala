package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.{BackgroundService, DirStats, DirStatsTask}
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import org.mikesajak.commander.util.PathUtils
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.concurrent.Service
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.{nested, sfxml}

trait DeletePanelController {
  def init(targetPaths: Seq[VPath], stats: DirStats, dialog: Dialog[ButtonType]): Service[DirStats]
  def dryRunSelected: Boolean
}

@sfxml
class DeletePanelControllerImpl(pathTypeLabel: Label,
                                pathToTargetLabel: Label,
                                targetNameLabel: Label,
                                pathsListView: ListView[String],
                                @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,
                                summaryMessageLabel: Label,
                                dryRunCheckbox: CheckBox,

                                resourceMgr: ResourceManager) extends DeletePanelController {

  override def init(targetPaths: Seq[VPath], stats: DirStats, dialog: Dialog[ButtonType]): Service[DirStats] = {
    val pathType = pathTypeOf(targetPaths)
    dialog.title = s"${resourceMgr.getMessage("app.name")} - ${resourceMgr.getMessage(s"delete_dialog.title")}"
    dialog.headerText = resourceMgr.getMessage(s"delete_dialog.header.${pathType.name}")
    dialog.graphic = new ImageView(resourceMgr.getIcon("delete-circle.png", IconSize.Big))
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Yes, ButtonType.No)
    pathTypeLabel.text = resourceMgr.getMessage(s"delete_dialog.to_delete.${pathType.name}")

    // create bindings - to resize parent layout on disable/hide
    pathsListView.managed <== pathsListView.visible

    val targetPath = targetPaths.head
    val targetParentName = targetPath.parent.map(_.absolutePath).getOrElse("")
    pathToTargetLabel.text = s"${PathUtils.shortenPathTo(targetParentName, 80)}/"
    pathToTargetLabel.tooltip = s"$targetParentName/"
    pathToTargetLabel.graphic = new ImageView(resourceMgr.getIcon(pathType.icon, IconSize.Small))

    if (pathType != MultiPaths) {
      targetNameLabel.text = targetPath.name
      pathsListView.visible = false
    } else {
      targetNameLabel.text = "[" + resourceMgr.getMessageWithArgs("delete_dialog.num_elements", Array(targetPaths.size)) + "]"
      pathsListView.visible = true
      pathsListView.items = ObservableBuffer(targetPaths.map(p => if (p.isDirectory) s"${p.name}/" else p.name))
    }

    summaryMessageLabel.text = resourceMgr.getMessage("delete_dialog.progress_not_available.label")
    summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("comment-alert-outline.png", IconSize.Small))
    summaryMessageLabel.tooltip = resourceMgr.getMessage("delete_dialog.progress_not_available.tooltip")

    statsPanelController.init(targetPaths)

    val statsService = new BackgroundService(new DirStatsTask(targetPaths))
    statsService.onRunning = e => statsPanelController.notifyStarted()
    statsService.onFailed = e => notifyError(Option(statsService.value.value), statsService.message.value)
    statsService.onSucceeded = e => notifyFinished(statsService.value.value, None)
    statsService.value.onChange { (_, _, stats) => statsPanelController.updateStats(stats, None)}

    dialog.onShown = e => statsService.start()

    statsService
  }

  override def dryRunSelected: Boolean = dryRunCheckbox.selected.value

  private def pathTypeOf(targetPaths: Seq[VPath]): PathType =
    targetPaths match {
      case p if p.size == 1 && p.head.isDirectory => SingleDir
      case p if p.size == 1 => SingleFile
      case p => MultiPaths
    }

  private def notifyFinished(stats: DirStats, message: Option[String]): Unit = {
    statsPanelController.notifyFinished(stats, message)

    summaryMessageLabel.text = resourceMgr.getMessage("delete_dialog.progress_available.label")
    summaryMessageLabel.graphic = null
    summaryMessageLabel.tooltip = resourceMgr.getMessage("delete_dialog.progress_available.tooltip")
  }

  private def notifyError(stats: Option[DirStats], message: String): Unit = {
    statsPanelController.notifyError(stats, message)

    summaryMessageLabel.text = message
    summaryMessageLabel.tooltip = "An error occurred while processing IO operation."
    summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("alert-circle.png", IconSize.Small))
  }
}
