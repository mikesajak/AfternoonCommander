package org.mikesajak.commander

import org.mikesajak.commander.fs.VDirectory

import scalafx.scene.control.{Label, TableColumn, TableView}
import scalafxml.core.macros.sfxml

/**
  * Created by mike on 14.04.17.
  */
@sfxml
class DirTableController(dirTableView: TableView[Any],
                         idTableColumn: TableColumn[Any, Any],
                         statusLabel1: Label,
                         statusLabel2: Label,
                         params: DirTableParams) {
  println(s"DirTableController: $params")
}

case class DirTableParams(path: VDirectory)


