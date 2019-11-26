package org.mikesajak.commander.task

import java.util.concurrent.FutureTask

import org.mikesajak.commander.ui.ResourceManager
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

//noinspection TypeAnnotation
class UserDecisionCtrl(resourceMgr: ResourceManager) {

  sealed abstract class ContinueDecisionButtonType(resourceKey: String) extends ButtonType(resourceMgr.getMessage(resourceKey))
  val RetryButtonType = new ContinueDecisionButtonType("decision_button.retry") {}
  val SkipButtonType = new ContinueDecisionButtonType("decision_button.skip") {}
  val AbortButtonType = new ContinueDecisionButtonType("decision_button.abort") {}

   def showErrorDialogAndAskForDecision(titleText: String, headerMsg: String, errorMsg: String): Option[ContinueDecisionButtonType]  = {
    val futureTask = new FutureTask(() =>
      new Alert(AlertType.Confirmation) {
        initOwner(null)
        title = titleText
        headerText = headerMsg
        contentText = errorMsg
        buttonTypes = Seq(RetryButtonType, SkipButtonType, AbortButtonType)
      }.showAndWait())
    Platform.runLater(futureTask)
    futureTask.get().asInstanceOf[Option[ContinueDecisionButtonType]]
  }


  sealed abstract class ConfirmButtonType(resourceKey: String) extends ButtonType(resourceMgr.getMessage(resourceKey))
  val YesButtonType = new ConfirmButtonType("confirm_button.yes") {}
  val YesToAllButtonType = new ConfirmButtonType("confirm_button.yes_to_all") {}
  val NoButtonType = new ConfirmButtonType("confirm_button.no") {}
  val NoToAllButtonType = new ConfirmButtonType("confirm_button.no_to_all") {}
  val CancelButtonType = new ConfirmButtonType("confirm_button.cancel") {}

  def showYesNoAllCancelDialog(titleText: String, headerMsg: String, description: String): Option[ConfirmButtonType] = {
    val futureTask = new FutureTask(() =>
      new Alert(AlertType.Confirmation) {
        initOwner(null)
        title = titleText
        headerText = headerMsg
        contentText = description
        buttonTypes = Seq(YesButtonType, YesToAllButtonType, NoButtonType, NoToAllButtonType, CancelButtonType)
      }.showAndWait())

    Platform.runLater(futureTask)
    futureTask.get().asInstanceOf[Option[ConfirmButtonType]]
  }
}
