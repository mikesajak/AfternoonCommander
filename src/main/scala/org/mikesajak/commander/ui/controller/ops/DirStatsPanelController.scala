package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.task.DirStats
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.UnitFormatter

import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait DirStatsPanelController {
  def init(targetPath: Seq[VPath], stats: Option[DirStats]): Unit
  def updateStats(stats: DirStats): Unit
}

@sfxml
class DirStatsPanelControllerImpl(dirSubdirsLabel: Label,
                                  dirFilesLabel: Label,
                                  dirModifiedLabel: Label,
                                  dirAttribsLabel: Label,

                                  resourceMgr: ResourceManager)
    extends DirStatsPanelController {
  println(s"DirStatsPanelControllerImpl constructor")

  override def init(targetPaths: Seq[VPath], stats: Option[DirStats]): Unit = {

    if (targetPaths.size == 1) {
      dirModifiedLabel.text = targetPaths.head.modificationDate.toString // TODO: format
      dirAttribsLabel.text = targetPaths.head.attribs
    } else {
      dirModifiedLabel.text = ""
      dirAttribsLabel.text = ""
    }

    dirSubdirsLabel.text = "..."
    dirFilesLabel.text = "..."

    stats.foreach(updateStats)
  }

  override def updateStats(stats: DirStats): Unit = {
    Platform.runLater {
      dirSubdirsLabel.text = resourceMgr.getMessageWithArgs("count_stats.num_directories",
                                                            Array(stats.numDirs, stats.depth))
      val unit = UnitFormatter.findDataSizeUnit(stats.size)
      dirFilesLabel.text = resourceMgr.getMessageWithArgs("count_stats.num_files",
                                                         Array(stats.numFiles, unit.convert(stats.size), unit.symbol))
    }
  }

}
