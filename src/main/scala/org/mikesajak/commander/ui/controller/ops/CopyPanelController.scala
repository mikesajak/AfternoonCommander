package org.mikesajak.commander.ui.controller.ops

import javafx.concurrent.Worker.State
import org.mikesajak.commander.fs.{VDirectory, VPath}
import org.mikesajak.commander.task.{BackgroundServiceRegistry, DirStats, DirStatsProcessor, DirWalkerTask}
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import org.mikesajak.commander.util.{PathUtils, Throttler}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.concurrent.Service
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}
import scribe.Logging

trait CopyPanelController {
  def initForCopy(sourcePaths: Seq[VPath], targetDir: VDirectory, dialog: Dialog[(String, Boolean)]): Service[DirStats] =
    init(sourcePaths, targetDir, "copy_dialog", dialog)

  def initForMove(sourcePaths: Seq[VPath], targetDir: VDirectory, dialog: Dialog[(String, Boolean)]): Service[DirStats] =
    init(sourcePaths, targetDir, "move_dialog", dialog)

  def init(sourcePaths: Seq[VPath], targetDir: VDirectory, dialogTypePrefix: String, dialog: Dialog[(String, Boolean)]): Service[DirStats]
}

@sfxml
class CopyPanelControllerImpl(sourcePathTypeLabel: Label,
                              pathToSourceLabel: Label,
                              sourceNameLabel: Label,
                              sourcePathsListView: ListView[String],
                              operationDestinationLabel: Label,
                              statsPanel: Pane,
                              @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,
                              targetDirCombo: ComboBox[String],
                              summaryMessageLabel: Label,
                              dryRunCheckbox: CheckBox,

                              resourceMgr: ResourceManager,
                              serviceRegistry: BackgroundServiceRegistry)
    extends CopyPanelController with Logging {

  private var dialogTypePrefix: String = _

  override def init(sourcePaths: Seq[VPath], targetDir: VDirectory, dialogTypePrefix: String, dialog: Dialog[(String, Boolean)]): Service[DirStats] = {
    val pathType = pathTypeOf(sourcePaths)
    this.dialogTypePrefix = dialogTypePrefix

    dialog.title = resourceMgr.getMessage(s"$dialogTypePrefix.title")
    dialog.headerText = resourceMgr.getMessage(s"$dialogTypePrefix.header.${pathType.name}")
    val headerIconName = resourceMgr.getMessage(s"$dialogTypePrefix.header.icon")
    dialog.graphic = new ImageView(resourceMgr.getIcon(headerIconName, IconSize.Big))
    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Yes, ButtonType.No)
    sourcePathTypeLabel.text = resourceMgr.getMessage(s"$dialogTypePrefix.source_type.${pathType.name}")
    operationDestinationLabel.text = resourceMgr.getMessage(s"$dialogTypePrefix.destination")

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
      sourceNameLabel.text = "[" + resourceMgr.getMessageWithArgs(s"$dialogTypePrefix.num_elements", Array(sourcePaths.size)) + "]"
      sourcePathsListView.visible = true
      sourcePathsListView.items = ObservableBuffer(sourcePaths.map(p => if (p.isDirectory) s"${p.name}/" else p.name) :_*)
    }

    summaryMessageLabel.text = resourceMgr.getMessage(s"$dialogTypePrefix.progress_not_available.label")
    summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("comment-alert-outline.png", IconSize.Small))
    summaryMessageLabel.tooltip = resourceMgr.getMessage(s"$dialogTypePrefix.progress_not_available.tooltip")

    statsPanelController.init(sourcePaths)

    val statsService = serviceRegistry.registerServiceFor(new DirWalkerTask(sourcePaths, new DirStatsProcessor()))

    statsService.state.onChange { (_, _, state) =>
      state match {
        case State.RUNNING => statsPanelController.notifyStarted()
        case State.FAILED => notifyError(Option(statsService.value.value), statsService.message.value)
        case State.SUCCEEDED => notifyFinished(statsService.value.value, None)
        case _ =>
      }
    }

    val throttler = new Throttler[DirStats](50, s => Platform.runLater(statsPanelController.updateStats(s, None)))
    Throttler.registerCancelOnServiceFinish(statsService, throttler)
    statsService.value.onChange { (_, _, stats) => throttler.update(stats) }

    dialog.onShown = _ => statsService.start()

    dialog.resultConverter = {
      case bt if bt == ButtonType.OK || bt == ButtonType.Yes =>
        (targetDirCombo.value.value, dryRunCheckbox.selected.value)
      case _ => null
    }

    Platform.runLater {
      targetDirCombo.requestFocus()
      val comboTF = targetDirCombo.editor.value
      comboTF.positionCaret(comboTF.text.value.length)
    }

    statsService
  }

  private def pathTypeOf(targetPaths: Seq[VPath]): PathType =
    targetPaths match {
      case p if p.size == 1 && p.head.isDirectory => SingleDir
      case p if p.size == 1 => SingleFile
      case _ => MultiPaths
    }

  private def notifyFinished(stats: DirStats, message: Option[String]): Unit = {
    statsPanelController.notifyFinished(stats, message)

    summaryMessageLabel.text = resourceMgr.getMessage(s"$dialogTypePrefix.progress_available.label")
    summaryMessageLabel.graphic = null
    summaryMessageLabel.tooltip = resourceMgr.getMessage(s"$dialogTypePrefix.progress_available.tooltip")
  }

  private def notifyError(stats: Option[DirStats], message: String): Unit = {
    statsPanelController.notifyError(stats, message)

    summaryMessageLabel.text = message
    summaryMessageLabel.tooltip = resourceMgr.getMessage(s"$dialogTypePrefix.io_error.tooltip")
    summaryMessageLabel.graphic = new ImageView(resourceMgr.getIcon("alert-circle.png", IconSize.Small))
  }
}

