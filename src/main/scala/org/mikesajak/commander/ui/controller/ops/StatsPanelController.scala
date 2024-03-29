package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.ui.controller.ops.StatsUtils.ContentType
import org.mikesajak.commander.ui.controller.ops.StatsUtils.ContentType.ContentType
import org.mikesajak.commander.units.DataUnit
import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml
import scribe.Logging

trait StatsPanelController {
  def init(targetPath: Seq[VPath]): Unit

  def notifyStarted(): Unit
  def notifyFinished(stats: DirStats, message: Option[String] = None): Unit
  def notifyError(stats: Option[DirStats], message: String): Unit
  def updateMessage(message: String): Unit
  def updateStats(stats: DirStats, message: Option[String]): Unit
}

package object StatsUtils {
  object ContentType extends Enumeration {
    type ContentType = Value
    val SingleFile, SingleDirectory, MultiPaths = Value
  }

  def resolveContentType(paths: Seq[VPath]): StatsUtils.ContentType.Value =
    if (paths.size == 1) {
      if (paths.head.isFile) ContentType.SingleFile else ContentType.SingleDirectory
    }
    else if (paths.size > 1) ContentType.MultiPaths
    else throw new IllegalArgumentException("Zero length path list provided")
}

@sfxml
class StatsPanelControllerImpl(messageLabel: Label,

                               modifiedLabel: Label,
                               modifiedValueLabel: Label,
                               attribsLabel: Label,
                               attribsValueLabel: Label,
                               sizeLabel: Label,
                               sizeValueLabel: Label,

                               directoriesLabel: Label,
                               filesLabel: Label,
                               directoriesValueLabel: Label,
                               filesValueLabel: Label,

                               resourceMgr: ResourceManager)
    extends StatsPanelController with Logging {

  override def init(targetPaths: Seq[VPath]): Unit = {
    val contentType = StatsUtils.resolveContentType(targetPaths)
    setupLabels(contentType)
    if (contentType == ContentType.SingleFile || contentType == ContentType.SingleDirectory)
      setProperties(targetPaths.head)
  }

  private def setupLabels(contentType: ContentType): Unit = {
    List(modifiedLabel, modifiedValueLabel, attribsLabel, attribsValueLabel,
         sizeLabel, sizeValueLabel, directoriesLabel, directoriesValueLabel,
         filesLabel, filesValueLabel)
      .foreach { e =>
        e.managed <== e.visible
      }

    modifiedLabel.visible = contentType != ContentType.MultiPaths
    modifiedValueLabel.visible = contentType != ContentType.MultiPaths
    attribsLabel.visible = contentType != ContentType.MultiPaths
    attribsValueLabel.visible = contentType != ContentType.MultiPaths

    sizeLabel.visible = contentType == ContentType.SingleFile
    sizeValueLabel.visible = contentType == ContentType.SingleFile

    directoriesLabel.visible = contentType != ContentType.SingleFile
    directoriesValueLabel.visible = contentType != ContentType.SingleFile
    filesLabel.visible = contentType != ContentType.SingleFile
    filesValueLabel.visible = contentType != ContentType.SingleFile
  }

  def setProperties(path: VPath): Unit = {
    Platform.runLater {
      modifiedValueLabel.text = path.modificationDate.toString // TODO: format
      attribsValueLabel.text = path.attributes.toString
      if (path.isFile)
        sizeValueLabel.text = DataUnit.formatDataSize(path.size.toDouble)
    }
  }


  override def notifyStarted(): Unit = {
    messageLabel.graphic = new ImageView(resourceMgr.getIcon("loading-chasing-arrows.gif"))
    messageLabel.text = resourceMgr.getMessage("stats_panel.counting.message.label")
  }

  override def notifyFinished(stats: DirStats, message: Option[String] = None): Unit = {
    updateStats(stats, message)
    messageLabel.graphic = null
    messageLabel.text = null
  }

  override def notifyError(stats: Option[DirStats], message: String): Unit = {
    stats match {
      case Some(s) => updateStats(s, Some(message))
      case _ => updateMessage(message)
    }
    messageLabel.graphic = null
  }

  override def updateMessage(message: String): Unit = {
    messageLabel.text = message
  }

  override def updateStats(stats: DirStats, message: Option[String]): Unit = {
    message.foreach(msg => messageLabel.text = msg)

    directoriesValueLabel.text = resourceMgr.getMessageWithArgs("stats_panel.num_directories.value.label",
      Seq(stats.numDirs, stats.depth))
    val unit = DataUnit.findDataSizeUnit(stats.size.toDouble)
    filesValueLabel.text = resourceMgr.getMessageWithArgs("stats_panel.num_files.value.label",
      Seq[Any](stats.numFiles, unit.convert(stats.size.toDouble), unit.symbol))
  }
}

