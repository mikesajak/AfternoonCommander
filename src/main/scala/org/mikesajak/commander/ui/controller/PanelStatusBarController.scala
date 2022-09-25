package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.fs.local.SymlinkPath
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.units.DataUnit
import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait PanelStatusBarController extends CurrentDirAware with CursorSelectionAware {
  def init(): Unit
  def setSelectedPaths(selectedPaths: Seq[VPath]): Unit
}

@sfxml
class PanelStatusBarControllerImpl(curSelectionLabel: Label,
                                   leftMessageLabel: Label,
                                   rightMessageLabel: Label,

                                   resourceMgr: ResourceManager) extends PanelStatusBarController {
  override def init(): Unit = {
    curSelectionLabel.styleClass += "file_panel_status_selected_path"
    leftMessageLabel.styleClass += "file_panel_status_dir_summary"
    rightMessageLabel.styleClass += "file_panel_status_dir_summary"
  }

  override def setDirectory(directory: VDirectory): Unit =
    leftMessageLabel.text = prepareSummary(directory.childDirs, directory.childFiles)

  override def setSelectedPaths(selectedPaths: Seq[VPath]): Unit = {
    val (dirs, files) = selectedPaths.partition(_.isDirectory)
    rightMessageLabel.text = prepareSummary(dirs.map(_.asInstanceOf[VDirectory]), files.map(_.asInstanceOf[VFile]))
    setSelectedPath(selectedPaths.headOption)
  }

  private def prepareSummary(dirs: Seq[VDirectory], files: Seq[VFile]): String = {
    val numDirs = dirs.size
    val totalSize = files.map(_.size).sum
    val sizeUnit = DataUnit.findDataSizeUnit(totalSize.toDouble)
    resourceMgr.getMessageWithArgs("file_table_panel.status.message",
                                   IndexedSeq[Any](numDirs, files.size, sizeUnit.convert(totalSize.toDouble),
                                                   sizeUnit.symbol))
  }

  override def setSelectedPath(path: Option[VPath]): Unit = {
    curSelectionLabel.text = path.map {
      case s: SymlinkPath => s"-> ${ s.target.absolutePath }"
      case p => p.name
    }.orNull
  }
}
