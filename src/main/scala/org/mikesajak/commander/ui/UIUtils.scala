package org.mikesajak.commander.ui

import javafx.scene.{control => jfxctrl, text => jfxtext}
import javafx.{geometry => jfxgeom}
import org.mikesajak.commander.util.Utils
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.Parent
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.layout.{GridPane, Region}
import scalafx.scene.text.Font
import scalafx.stage.{Modality, Stage, StageStyle}

object UIParams {
  val NumPrevVisibleItems: Int = 5
}

object UIUtils {
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

  def mkModalDialogNoButtonOrder[ResultType](owner: Stage, content: Parent): Dialog[ResultType] = new Dialog[ResultType]() {
    initOwner(owner)
    initStyle(StageStyle.Utility)
    initModality(Modality.ApplicationModal)
    dialogPane = mkDialogPaneWithNoButtonOrder()
    dialogPane().content = content
  }

  def mkDialogPaneWithNoButtonOrder() =
    new DialogPane(new jfxctrl.DialogPane() {
      override def createButtonBar(): jfxctrl.ButtonBar = {
        val bar = super.createButtonBar().asInstanceOf[jfxctrl.ButtonBar]
        bar.setButtonOrder(jfxctrl.ButtonBar.BUTTON_ORDER_NONE)
        bar
      }
    })

  def dialogButton[R](dialog: Dialog[R], buttonType: ButtonType): Button =
    dialog.dialogPane.value.lookupButton(buttonType).asInstanceOf[jfxctrl.Button]

  def calcTextBounds(control: TextInputControl): jfxgeom.Bounds =
    calcTextBounds(control.text.value, control.font.value, Option(control.insets))

  def calcTextBounds(text: String, font: Font, padding: Option[Insets] = None): jfxgeom.Bounds = {
    val textElement = new jfxtext.Text(text)
    textElement.font = font
    textElement.applyCss()
    val textBounds = textElement.getLayoutBounds

    padding.map(p => new jfxgeom.BoundingBox(textBounds.getMinX - p.left, textBounds.getMinY - p.top,
                                             textBounds.getWidth + p.left + p.right,
                                             textBounds.getHeight + p.top + p.bottom))
           .getOrElse(textBounds)
  }
}