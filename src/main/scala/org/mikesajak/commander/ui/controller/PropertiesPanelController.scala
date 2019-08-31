package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.controller.properties._
import org.mikesajak.commander.ui.{IconResolver, IconSize}
import scalafx.scene.control.Label
import scalafxml.core.macros.{nested, sfxml}

trait PropertiesPanelController {
  def init(path: VPath)
}

@sfxml
class PropertiesPanelControllerImpl(nameLabel: Label,
                                    iconLabel: Label,
                                    @nested[GeneralPropertiesPanelControllerImpl] generalPropertiesPanelController: GeneralPropertiesPanelController,
                                    @nested[FileContentPropertiesPanelControllerImpl] fileContentPropertiesPanelController: FileContentPropertiesPanelController,
                                    @nested[DirContentPropertiesPanelControllerImpl] dirContentPropertiesPanelController: DirContentPropertiesPanelController,

                                    fileTypeManager: FileTypeManager,
                                    iconResolver: IconResolver)
    extends PropertiesPanelController {

  override def init(path: VPath): Unit = {
    nameLabel.text = path.name

    iconLabel.text = ""
    iconLabel.graphic = iconResolver.findIconFor(path, IconSize(48)).orNull

    generalPropertiesPanelController.init(path)
    path match {
      case f: VFile => fileContentPropertiesPanelController.init(f)
      case d: VDirectory => dirContentPropertiesPanelController.init(d)
    }
  }
}
