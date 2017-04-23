package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.ResourceManager

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
  val size = new StringProperty((if (path.isFile) path.asInstanceOf[VFile].size else 0).toString)
  val modifiyDate = new StringProperty(path.modificationDate.toString)
  val attributes = new StringProperty(path.attribs)

  def mkName(p: VPath): String = if (p.isDirectory) s"[${p.name}]" else p.name

  override def toString: String = s"FileRow(path=$path, $name, $extension, $size, $modifiyDate, $attributes)"
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
                         params: DirTableParams,

                         fileTypeManager: FileTypeManager,
                         resourceManager: ResourceManager) {

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
  nameColumn.cellValueFactory = { _.value.name }
  sizeColumn.cellValueFactory = {_.value.size }
  dateColumn.cellValueFactory = { _.value.modifiyDate }
  attribsColumn.cellValueFactory = { _.value.attributes }

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

  initTable(params.path)

  def initTable(directory: VDirectory) {
    val (dirs0, files0) = directory.children.partition(p => p.isDirectory)

    val dirs =
      (if (directory.parent.isDefined) Seq(new PathToParent(directory)) else Seq()) ++
      dirs0.sortBy(d => d.name)
    val files = files0.sortBy(f => f.name)

    val fileRows =
      dirs.map(d => new FileRow(d)) ++
      files.map(f => new FileRow(f))

    dirTableView.items = ObservableBuffer(fileRows)
  }

  def handleAction(path: VPath): Unit = {
    if (path.isDirectory)
      initTable(path.directory)
    else {
      val fileType = fileTypeManager.detectFileType(path)
      val fileTypeActionHandler = fileTypeManager.fileTypeHandler(path)
      println(s"TODO: file action $path, fileType=$fileType, fileTypeActionHandler=$fileTypeActionHandler")
    }
  }

}

case class DirTableParams(path: VDirectory)
