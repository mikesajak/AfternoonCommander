package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.units.DataUnit
import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait PanelStatusBarController extends CurrentDirAware {
  def init(): Unit
  def setSelectedPaths(selectedPaths: Seq[VPath]): Unit
}

@sfxml
class PanelStatusBarControllerImpl(leftMessageLabel: Label,
                                   rightMessageLabel: Label,

                                   resourceMgr: ResourceManager) extends PanelStatusBarController {
  override def init(): Unit = {
    leftMessageLabel.styleClass += "file_panel_status"
    rightMessageLabel.styleClass += "file_panel_status"
  }

  override def setDirectory(directory: VDirectory): Unit =
    leftMessageLabel.text = prepareSummary(directory.childDirs, directory.childFiles)

  override def setSelectedPaths(selectedPaths: Seq[VPath]): Unit = {
    val (dirs, files) = selectedPaths.partition(_.isDirectory)
    rightMessageLabel.text = prepareSummary(dirs.map(_.asInstanceOf[VDirectory]), files.map(_.asInstanceOf[VFile]))
  }

  private def prepareSummary(dirs: Seq[VDirectory], files: Seq[VFile]): String = {
    val numDirs = dirs.size
    val totalSize = files.map(_.size).sum
    val sizeUnit = DataUnit.findDataSizeUnit(totalSize.toDouble)
    resourceMgr.getMessageWithArgs("file_table_panel.status.message",
                                   IndexedSeq[Any](numDirs, files.size, sizeUnit.convert(totalSize.toDouble),
                                                   sizeUnit.symbol))
  }

}
