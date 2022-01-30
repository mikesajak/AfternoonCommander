package org.mikesajak.commander.ui.controller.properties

import javafx.concurrent.Worker.State
import org.apache.commons.io.FilenameUtils
import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.{BackgroundService, DirContents, DirStats}
import org.mikesajak.commander.ui.MyScalaFxImplicits._
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.units.DataUnit
import org.mikesajak.commander.util.Throttler
import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml
import scribe.Logging

import java.time.ZoneId
import java.time.format.{DateTimeFormatter, FormatStyle}

trait GeneralPropertiesPanelController {
  def init(path: VPath, statsService: BackgroundService[(String, DirStats, DirContents)]): Unit
}

@sfxml
class GeneralPropertiesPanelControllerImpl(pathLabel: Label,
                                           nameLabel: Label,
                                           typeLabel: Label,
                                           modifiedLabel: Label,
                                           createdLabel: Label,
                                           lastAccessedLabel: Label,
                                           attributesLabel: Label,
                                           sizeLabel: Label,
                                           numDirsLabel: Label,
                                           numDirsNameLabel: Label,
                                           numFilesLabel: Label,
                                           numFilesNameLabel: Label,

                                           fileTypeManager: FileTypeManager,
                                           resourceMgr: ResourceManager)
    extends GeneralPropertiesPanelController with Logging {

  private val timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG)
                                               .withZone(ZoneId.systemDefault())

  override def init(path: VPath, statsService: BackgroundService[(String, DirStats, DirContents)]): Unit = {
    pathLabel.text = FilenameUtils.getFullPath(path.absolutePath)
    nameLabel.text = FilenameUtils.getName(path.absolutePath)
    val fileType = fileTypeManager.detectFileType(path)
    val fileTypeName = fileTypeManager.descriptionOf(fileType)
    val mimeType = fileTypeManager.mimeTypeOf(path)
    typeLabel.text = s"$fileTypeName ($mimeType)"

    modifiedLabel.text = timeFormatter.format(path.modificationDate)
    createdLabel.text = timeFormatter.format(path.creationDate)
    lastAccessedLabel.text = timeFormatter.format(path.accessDate)

    attributesLabel.text = path.attributes.toString

    if (path.isFile) {
      sizeLabel.text = DataUnit.mkDataSize(path.size).toString
      (numDirsNameLabel, numDirsLabel, numFilesNameLabel, numFilesLabel).visible = false
    } else {
      (sizeLabel, numDirsLabel, numFilesLabel).text = ""
    }

    val throttler = new Throttler[DirStats](50, stats => Platform.runLater(updateStats(stats)))
    Throttler.registerCancelOnServiceFinish(statsService, throttler)
    statsService.value.onChange { (_, _, stats) => throttler.update(stats._2) }

    statsService.state.onChange { (_, _, state) =>
      state match {
        case State.RUNNING =>   notifyStarted()
        case State.FAILED =>    notifyError(Option(statsService.value.value._2), statsService.message.value)
        case State.SUCCEEDED => notifyFinished(statsService.value.value._2)
        case _ =>
      }
    }
    logger.debug("GeneralPropertiesPanelController initialized.")
  }

  private def updateStats(stats: DirStats): Unit = {
    numDirsLabel.text = resourceMgr.getMessageWithArgs("properties_panel.general_tab.num_directories.value.label",
                                                       Array(stats.numDirs, stats.depth))
    numFilesLabel.text = s"${stats.numFiles}"

    val unit = DataUnit.findDataSizeUnit(stats.size)
    sizeLabel.text = resourceMgr.getMessageWithArgs("properties_panel.general_tab.size.value.label",
                                                    Array[Any](unit.convert(stats.size), unit.symbol, stats.size))
  }

  def notifyStarted(): Unit = {
  }

  def notifyFinished(stats: DirStats): Unit = {
    updateStats(stats)
  }

  def notifyError(stats: Option[DirStats], message: String): Unit = {
    logger.warn(s"Stats collecting service reported an error: $message. Stats: $stats")
    stats.foreach(s => updateStats(s))
  }
}
