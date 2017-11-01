package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.util.UnitFormatter

import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait DirStatsPanelController {
  def init(targetPath: VPath, stats: Option[DirStats]): Unit
  def updateStats(stats: DirStats): Unit
}

@sfxml
class DirStatsPanelControllerImpl(dirSubdirsLabel: Label,
                                  dirFilesLabel: Label,
                                  dirModifiedLabel: Label,
                                  dirAttribsLabel: Label)
    extends DirStatsPanelController {
  println(s"DirStatsPanelControllerImpl constructor")

  override def init(targetPath: VPath, stats: Option[DirStats]): Unit = {

    dirModifiedLabel.text = targetPath.modificationDate.toString // TODO: format
    dirAttribsLabel.text = targetPath.attribs

    dirSubdirsLabel.text = "..."
    dirFilesLabel.text = "..."

    stats.foreach(updateStats)
  }

  override def updateStats(stats: DirStats): Unit = {
    dirSubdirsLabel.text = s"${stats.numDirs} (depth: ${stats.depth} levels)"
    dirFilesLabel.text = s"${stats.numFiles} (size: ${UnitFormatter.formatUnit(stats.size)})"
  }

}
