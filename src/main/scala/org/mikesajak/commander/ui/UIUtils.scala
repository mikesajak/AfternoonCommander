package org.mikesajak.commander.ui

import javafx.geometry.{BoundingBox, Bounds}
import javafx.scene.control.IndexedCell
import javafx.scene.text.Text

import com.sun.javafx.scene.control.skin.{TableViewSkin, VirtualFlow}
import org.mikesajak.commander.util.Utils

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.layout.{GridPane, Region}
import scalafx.scene.text.Font
import scalafx.scene.{Node, Parent}
import scalafx.stage.{Modality, Stage, StageStyle, Window}

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
                            exception: Throwable): Alert = {
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
        minWidth = 300
        minHeight = Region.USE_PREF_SIZE
        maxWidth = Double.MaxValue
        maxHeight = Double.MaxValue
        prefWidth = Region.USE_COMPUTED_SIZE
        prefHeight = Region.USE_COMPUTED_SIZE
      }
      dialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE)
      dialogPane().setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE)
    }
  }

  def mkModalDialog[ResultType](owner: Stage, content: Parent): Dialog[ResultType] = new Dialog[ResultType]() {
    initOwner(owner)
    initStyle(StageStyle.Utility)
    initModality(Modality.ApplicationModal)
    dialogPane().content = content
  }

  implicit def jfxButton2SfxButton(button: javafx.scene.control.Button): Button = new Button(button)

  def calcTextBounds(control: TextInputControl): Bounds =
    calcTextBounds(control.text.value, control.font.value, Option(control.insets))

  def calcTextBounds(text: String, font: Font, padding: Option[Insets] = None): Bounds = {
    val textElement = new Text(text)
    textElement.font = font
    textElement.applyCss()
    val textBounds = textElement.getLayoutBounds

    padding.map(p => new BoundingBox(textBounds.getMinX - p.left, textBounds.getMinY - p.top,
                                     textBounds.getWidth + p.left + p.right,
                                     textBounds.getHeight + p.top + p.bottom))
      .getOrElse(textBounds)
  }

  def showButtonCtxMenu(button: Button, ctxMenu: ContextMenu, owner: Node): Unit = {
    val bounds = button.localToScreen(button.boundsInLocal.value)
    ctxMenu.show(owner, bounds.getMinX, bounds.getMaxY)
  }

  def showButtonCtxMenu(button: Button, ctxMenu: ContextMenu, owner: Window): Unit = {
    val bounds = button.localToScreen(button.boundsInLocal.value)
    ctxMenu.show(owner, bounds.getMinX, bounds.getMaxY)
  }
}