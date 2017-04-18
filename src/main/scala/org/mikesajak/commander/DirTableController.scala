package org.mikesajak.commander

import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}

import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{Label, TableColumn, TableView}
import scalafxml.core.macros.sfxml

/**
  * Created by mike on 14.04.17.
  */
class FileRow(path: VPath) {
  val name = new StringProperty(mkName(path))
  val extension = new StringProperty("")
  val size = new StringProperty((if (path.isFile) path.asInstanceOf[VFile].size else 0).toString)
  val modifiyDate = new StringProperty(path.modificationDate.toString)
  val attributes = new StringProperty(path.attribs)

  def mkName(p: VPath) = if (p.isDirectory) s"[${p.name}]" else p.name
}

@sfxml
class DirTableController(dirTableView: TableView[Any],
                         idTableColumn: TableColumn[Any, Any],
                         table: TableView[FileRow],
                         nameColumn: TableColumn[FileRow, String],
                         extensionColumn: TableColumn[FileRow, String],
                         sizeColumn: TableColumn[FileRow, String],
                         dateColumn: TableColumn[FileRow, String],
                         attribsColumn: TableColumn[FileRow, String],
                         statusLabel1: Label,
                         statusLabel2: Label,
                         params: DirTableParams) {
  println(s"DirTableController: $params")

  nameColumn.cellValueFactory = { _.value.name }
  sizeColumn.cellValueFactory = {_.value.size }
  dateColumn.cellValueFactory = { _.value.modifiyDate }
  attribsColumn.cellValueFactory = { _.value.attributes }

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

}

case class DirTableParams(path: VDirectory)
