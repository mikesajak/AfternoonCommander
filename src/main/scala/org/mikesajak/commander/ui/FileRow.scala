package org.mikesajak.commander.ui

import javafx.beans.property.{SimpleLongProperty, SimpleStringProperty}

import org.mikesajak.commander.fs.{VDirectory, VPath}

import scalafx.beans.property.StringProperty
import scalafx.scene.control.{TableColumn, TableView}

/**
  * Created by mike on 09.04.17.
  */
class FileRow {
  val name = new StringProperty("")
  val extension = new StringProperty("")
  val size = new StringProperty("")
  val modifiyDate = new StringProperty("")
  val attributes = new StringProperty("")
}

class FileTableController(private val table: TableView[FileRow],
                          private val nameColumn: TableColumn[FileRow, String],
                          private val extensionColumn: TableColumn[FileRow, String],
                          private val sizeColumn: TableColumn[FileRow, String],
                          private val modifyColumn: TableColumn[FileRow, String],
                          private val attributesColumn: TableColumn[FileRow, String]) {
  nameColumn.cellValueFactory = { _.value.name }
  sizeColumn.cellValueFactory = {_.value.size }
  modifyColumn.cellValueFactory = { _.value.modifiyDate }
  attributesColumn.cellValueFactory = { _.value.attributes }

}
