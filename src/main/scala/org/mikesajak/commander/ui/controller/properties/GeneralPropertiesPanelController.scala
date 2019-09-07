package org.mikesajak.commander.ui.controller.properties

import com.typesafe.scalalogging.Logger
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

trait GeneralPropertiesPanelController {
  def init(path: VPath, statsService: BackgroundService[(DirStats, DirContents)])
}

@sfxml
class GeneralPropertiesPanelControllerImpl(pathLabel: Label,
                                           nameLabel: Label,
                                           typeLabel: Label,
                                           modifiedLabel: Label,
                                           attributesLabel: Label,
                                           sizeLabel: Label,
                                           numDirsLabel: Label,
                                           numDirsNameLabel: Label,
                                           numFilesLabel: Label,
                                           numFilesNameLabel: Label,

                                           fileTypeManager: FileTypeManager,
                                           resourceMgr: ResourceManager)
    extends GeneralPropertiesPanelController {
  private val logger = Logger[GeneralPropertiesPanelControllerImpl]

  logger.debug("GeneralPropertiesPanelController constructor...")

  override def init(path: VPath, statsService: BackgroundService[(DirStats, DirContents)]): Unit = {
    pathLabel.text = FilenameUtils.getFullPath(path.absolutePath)
    nameLabel.text = FilenameUtils.getName(path.absolutePath)
    val fileType = fileTypeManager.detectFileType(path)
    val fileTypeName = fileTypeManager.descriptionOf(fileType)
    val mimeType = fileTypeManager.mimeTypeOf(path)
    typeLabel.text = s"$fileTypeName ($mimeType)"

    modifiedLabel.text = path.modificationDate.toString
    attributesLabel.text = path.attributes.toString

    if (path.isFile) {
      sizeLabel.text = DataUnit.mkDataSize(path.size).toString
      (numDirsNameLabel, numDirsLabel, numFilesNameLabel, numFilesLabel).visible = false
    } else {
      (sizeLabel, numDirsLabel, numFilesLabel).text = ""
    }

    val throttler = new Throttler[DirStats](50, stats => Platform.runLater(updateStats(stats)))
    statsService.value.onChange { (_, _, stats) => throttler.update(stats._1) }

    statsService.state.onChange { (_, _, state) =>
      state match {
        case State.RUNNING =>
          throttler.cancel()
          notifyStarted()
        case State.FAILED =>
          throttler.cancel()
          notifyError(Option(statsService.value.value._1), statsService.message.value)
        case State.SUCCEEDED =>
          throttler.cancel()
          notifyFinished(statsService.value.value._1, None)
        case _ =>
      }
    }
    logger.debug("GeneralPropertiesPanelController initialized.")
  }

  private def updateStats(stats: DirStats): Unit = {
    numDirsLabel.text = resourceMgr.getMessageWithArgs("properties_panel.general.num_directories.value.label",
                                                       Array(stats.numDirs, stats.depth))
    numFilesLabel.text = s"${stats.numFiles}"

    val unit = DataUnit.findDataSizeUnit(stats.size)
    sizeLabel.text = resourceMgr.getMessageWithArgs("properties_panel.general.size.value.label",
                                                    Array(unit.convert(stats.size), unit.symbol, stats.size))
  }

  def notifyStarted(): Unit = {
  }

  def notifyFinished(stats: DirStats, message: Option[String] = None): Unit = {
    updateStats(stats)
  }

  def notifyError(stats: Option[DirStats], message: String): Unit = {
    stats.foreach(s => updateStats(s))
  }
}
