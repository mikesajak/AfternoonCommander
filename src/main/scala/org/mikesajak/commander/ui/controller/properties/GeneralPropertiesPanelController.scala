package org.mikesajak.commander.ui.controller.properties

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.ui.controller.ops.{StatsPanelController, StatsPanelControllerImpl}
import org.mikesajak.commander.units.DataUnit
import scalafx.scene.control.Label
import scalafxml.core.macros.{nested, sfxml}

trait GeneralPropertiesPanelController {
  def init(path: VPath)
}

@sfxml
class GeneralPropertiesPanelControllerImpl(pathLabel: Label,
                                           typeLabel: Label,
                                           sizeLabel: Label,
                                           modificationLabel: Label,
                                           attributesLabel: Label,
                                           @nested[StatsPanelControllerImpl] statsPanelController: StatsPanelController,

                                           fileTypeManager: FileTypeManager)
  extends GeneralPropertiesPanelController {
  private val logger = Logger[GeneralPropertiesPanelControllerImpl]

  override def init(path: VPath): Unit = {
    pathLabel.text = path.absolutePath
    val fileType = fileTypeManager.detectFileType(path)
    val fileTypeName = fileTypeManager.descriptionOf(fileType)
    val mimeType = fileTypeManager.mimeTypeOf(path)
    typeLabel.text = s"$fileTypeName ($mimeType)"

    sizeLabel.text = DataUnit.mkDataSize(path.size).toString
    modificationLabel.text = path.modificationDate.toString
    attributesLabel.text = path.attributes.toString

    statsPanelController.init(Seq(path))
  }
}
