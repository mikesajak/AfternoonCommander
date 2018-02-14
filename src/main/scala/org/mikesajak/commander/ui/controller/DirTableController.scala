package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.UnitFormatter

import scalafx.Includes._
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.input.{KeyEvent, MouseButton, MouseEvent}
import scalafxml.core.macros.sfxml

/**
  * Created by mike on 14.04.17.
  */
class FileRow(val path: VPath) {
  val name = new StringProperty(mkName(path))
  val extension = new StringProperty("")
  val size = new StringProperty(formatSize(path))
  val modifiyDate = new StringProperty(path.modificationDate.toString)
  val attributes = new StringProperty(path.attribs)

  def mkName(p: VPath): String = if (p.isDirectory) s"[${p.name}]" else p.name

  def formatSize(vFile: VPath): String =
    path match {
      case p: PathToParent => "PARENT"
      case p: VDirectory => UnitFormatter.formatNumElements(path.directory.children.size)
      case p: VFile => UnitFormatter.formatUnit(path.size, false)
    }

  override def toString: String = s"FileRow(path=$path, $name, $extension, $size, $modifiyDate, $attributes)"
}

trait DirTableControllerIntf {
  def init(panelController: DirPanelControllerIntf, path: VDirectory)
  def focusedRow: FileRow
  def focusedPath: VPath = focusedRow.path
  def selectedRows: Seq[FileRow]
  def selectedPaths: Seq[VPath] = selectedRows.map(_.path)
  def reload(): Unit
  def select(fileName: String): Unit
}

@sfxml
class DirTableController(dirTableView: TableView[FileRow],
                         idColumn: TableColumn[FileRow, VPath],
                         nameColumn: TableColumn[FileRow, String],
                         extensionColumn: TableColumn[FileRow, String],
                         sizeColumn: TableColumn[FileRow, String],
                         dateColumn: TableColumn[FileRow, String],
                         attribsColumn: TableColumn[FileRow, String],
                         statusLabel1: Label,
                         statusLabel2: Label,

                         fileTypeManager: FileTypeManager,
                         resourceManager: ResourceManager,
                         config: Configuration)
    extends DirTableControllerIntf {

  import org.mikesajak.commander.ui.UIParams._

  private var curDir: VDirectory = _
  private var panelController: DirPanelControllerIntf = _

  override def focusedRow: FileRow = dirTableView.selectionModel.value.getSelectedItem

  override def selectedRows: Seq[FileRow] = dirTableView.selectionModel.value.getSelectedItems.toList

  override def init(dirPanelController: DirPanelControllerIntf, path: VDirectory) {
    curDir = path
    this.panelController = dirPanelController

    idColumn.cellValueFactory = { t => ObjectProperty(t.value.path) }
    idColumn.cellFactory = { tc: TableColumn[FileRow, VPath] =>
      new TableCell[FileRow, VPath]() {
        item.onChange { (observable, oldValue, newValue) =>
          val fileType = fileTypeManager.detectFileType(newValue)
          val typeIcon = fileTypeManager.getIcon(fileType)
          val imageView = typeIcon.map { icon =>
            val imageView = new ImageView(resourceManager.getIcon(icon, 18, 18))
            imageView.preserveRatio = true
            imageView
          }.orNull
          graphic = imageView
        }
      }
    }
    nameColumn.cellValueFactory = {
      _.value.name
    }
    sizeColumn.cellValueFactory = {
      _.value.size
    }
    dateColumn.cellValueFactory = {
      _.value.modifiyDate
    }
    attribsColumn.cellValueFactory = {
      _.value.attributes
    }

    dirTableView.rowFactory = { tableView =>
      val row = new TableRow[FileRow]()
      row.handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
        if (!row.isEmpty && event.button == MouseButton.Primary && event.clickCount == 2) {
          handleAction(row.item.value.path)
        }
      }

      row
    }

    dirTableView.handleEvent(KeyEvent.KeyTyped) { event: KeyEvent =>
      if (event.character.contains("\n") || event.character.contains("\r")) {
        val items = dirTableView.selectionModel.value.selectedItems
        if (items.nonEmpty) {
          handleAction(items.head.path)
        }
      }
    }

    initTable(path, None)
  }

  override def reload(): Unit = {
    initTable(curDir, Some(focusedPath))
  }

  override def select(target: String): Unit = {
    val selIndex = {
      val idx = dirTableView.items.value.map(fileRow => fileRow.path.name).indexOf(target)
      if (idx > 0) idx else 0
    }

    selectIndex(selIndex)
  }

  private def handleAction(path: VPath): Unit = {
    if (path.isDirectory)
      changeDir(path.directory)
    else {
      val fileType = fileTypeManager.detectFileType(path)
      val fileTypeActionHandler = fileTypeManager.fileTypeHandler(path)
      println(s"TODO: file action $path, fileType=$fileType, fileTypeActionHandler=$fileTypeActionHandler")
    }
  }

  private def changeDir(directory: VDirectory): Unit = {
    curDir = directory
    updateParentTab(directory)
    val selection = directory match {
      case p: PathToParent => Some(p.targetDir)
      case _ => None
    }
    initTable(directory, selection)
  }

  private def updateParentTab(directory: VDirectory): Unit = {
    val targetDir = directory match {
      case p: PathToParent => p.targetDir
      case _ => directory
    }
    panelController.updateCurTab(targetDir)
  }

  private def initTable(directory: VDirectory, selection: Option[VPath]) {
    val (dirs0, files0) = directory.children.partition(p => p.isDirectory)

    val showHidden= config.boolProperty("filePanels", "showHiddenFiles").getOrElse(true)

    val dirs =
      (if (directory.parent.isDefined) Seq(new PathToParent(directory)) else Seq()) ++
      dirs0.sortBy(d => d.name)
    val files = files0.sortBy(f => f.name)

    val fileRows = (dirs ++ files).view
      .filter(f => showHidden || f.attribs.contains('h'))
      .map(f => new FileRow(f))
      .toList

    dirTableView.items = ObservableBuffer(fileRows)

//    val fromDir = directory match {
//      case p: PathToParent => Some(p.curDir)
//      case _ => None
//    }
    val fromDir = selection

    val selIndex = fromDir.map { prevDir =>
      val idx = dirs.map(_.name).indexOf(prevDir.name)
      if (idx > 0) idx else 0
    }.getOrElse(0)

    selectIndex(selIndex)
  }

  private def selectIndex(selIndex: Int): Unit = {
    val prevSelection = dirTableView.getSelectionModel.getSelectedIndex
    if (prevSelection != selIndex) {
      dirTableView.getSelectionModel.select(selIndex)
      dirTableView.scrollTo(math.max(selIndex - NumPrevVisibleItems, 0))
    }
  }
}
