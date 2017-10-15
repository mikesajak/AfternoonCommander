package org.mikesajak.commander.ui

import javafx.scene.control.IndexedCell

import com.sun.javafx.scene.control.skin.{TableViewSkin, VirtualFlow}
import org.mikesajak.commander.util.Utils

import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, Label, TableView, TextArea}
import scalafx.scene.layout.{GridPane, Region}
import scalafx.stage.Stage

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

  def prepareExceptionAlert(ownerWindow: Stage,
                            alertTitle: String,
                            alertHeader: String,
                            alertContent: String,
                            exception: Exception): Alert = {
    new Alert(AlertType.Error) {
      initOwner(ownerWindow)
      title = alertTitle
      headerText = alertHeader
      contentText = alertContent

      resizable = true

      // todo: find how to resize dialog window when content is ontracted (UGH, dialog does not shrink!)
      dialogPane().expandableContent = new GridPane() {
        add(new Label("The exception stack trace:"), 0, 0)
        add(new TextArea {
          text = Utils.getStackTraceText(exception)
          editable = false
          wrapText = true
        }, 0, 1)
      }
      dialogPane().setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
    }
  }
}