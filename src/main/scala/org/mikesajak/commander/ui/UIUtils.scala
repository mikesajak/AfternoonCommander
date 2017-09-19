package org.mikesajak.commander.ui

import javafx.scene.control.IndexedCell

import com.sun.javafx.scene.control.skin.{TableViewSkin, VirtualFlow}

import scalafx.scene.control.TableView

object UIParams {
  val NumPrevVisibleItems: Int = 5
}

object UIUtils {
  def getNumVisibleRows[S](tableView: TableView[S]): Int = {
    val skin = tableView.delegate.getSkin.asInstanceOf[TableViewSkin[S]]
    if (skin != null) {
      val vflow = skin.getChildren.get(1).asInstanceOf[VirtualFlow[IndexedCell[S]]]
      vflow.getLastVisibleCell.getIndex
    } else -1
  }
}