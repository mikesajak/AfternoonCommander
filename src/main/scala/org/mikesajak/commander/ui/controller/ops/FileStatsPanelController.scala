package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.util.UnitFormatter._

import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait FileStatsPanelController {
  def init(targetPath: VPath): Unit
}

@sfxml
class FileStatsPanelControllerImpl(fileSizeLabel: Label,
                                   fileModifiedLabel: Label,
                                   fileAttribsLabel: Label) extends FileStatsPanelController {
  def init(targetPath: VPath): Unit = {
    fileSizeLabel.text = formatDataSize(targetPath.size)
    fileModifiedLabel.text = targetPath.modificationDate.toString // TODO: format
    fileAttribsLabel.text = targetPath.attribs
  }

}