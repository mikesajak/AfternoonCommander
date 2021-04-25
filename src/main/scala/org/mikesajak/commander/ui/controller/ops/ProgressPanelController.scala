package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.task.{BackgroundService, IOProgress, IOTaskSummary}
import org.mikesajak.commander.ui.UIUtils
import org.mikesajak.commander.units.{DataUnit, TimeInterval}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml
import scribe.Logging

import java.util.{Timer, TimerTask}
import scala.language.implicitConversions

trait ProgressPanelController {
  def init(title: String, headerText: String, statusMessage: String,
           operationIcon: Image, dialog: Dialog[IOTaskSummary],
           workerService: BackgroundService[IOProgress])
}

@sfxml
class ProgressPanelControllerImpl(nameLabel: Label,
                                  detailsLabel: Label,
                                  totalProgressIndicator: ProgressIndicator,
                                  progressBar: ProgressBar,
                                  progressLabel: Label,
                                  fileCountLabel: Label,
                                  dirCountLabel: Label,
                                  sizeLabel: Label,
                                  elapsedTimeLabel: Label,
                                  estimatedTimeLabel: Label,
                                  speedLabel: Label,
                                  dontCloseCheckbox: CheckBox)
    extends ProgressPanelController with Logging {

  private var dialog: Dialog[IOTaskSummary] = _

  private var startTime: Long = _
  private val timer = new Timer()

  override def init(title: String, headerText: String, statusMessage: String,
                    operationIcon: Image, dialog: Dialog[IOTaskSummary],
                    workerService: BackgroundService[IOProgress]): Unit = {
    this.dialog = dialog

    dialog.title = title
    dialog.headerText = headerText
    dialog.graphic = new ImageView(operationIcon)

    nameLabel.text= statusMessage

    workerService.value.onChange { (_,_,value) =>
      updateValue(value)
      updateProgress(value)
    }

    workerService.onSucceeded = { _ => updateFinished() }
    workerService.onFailed = { _ => updateFinished() }

    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Cancel)

    val cancelButton = UIUtils.dialogButton(dialog, ButtonType.Cancel)
    cancelButton.onAction = { _ =>
      logger.info(s"Cancelling operation: ${workerService.title}")
      workerService.cancel()
      timer.cancel()
    }

    dialog.onShowing = _ => dialog.getDialogPane.getScene.getWindow.sizeToScene()

    dialog.onShown = { _ =>
      startTime = System.currentTimeMillis()
      timer.scheduleAtFixedRate(new TimerTask {
        override def run(): Unit = {
          Platform.runLater(updateTimes(Some(workerService)))
        }
      }, 1000, 1000)
      workerService.start()
    }

    dialog.onHidden = _ => timer.cancel()

    dialog.resultConverter = {
      case ButtonType.OK => workerService.getValue.summary
      case ButtonType.Close => workerService.getValue.summary
      case ButtonType.Cancel => null
    }
  }

  private def updateFinished(): Unit = {
    progressBar.progress = 1.0
    totalProgressIndicator.progress = 1.0

    timer.cancel()
    updateTimes(None)

    dialog.getDialogPane.buttonTypes = Seq(ButtonType.Close)

    if (!dontCloseCheckbox.isSelected) {
      val closeButton = UIUtils.dialogButton(dialog, ButtonType.Close)
      closeButton.fire()
    }
  }

  private def updateProgress(ioProgress: IOProgress): Unit = {
    val totalProgressValue = ioProgress.jobStats.map(s => IOProgress.calcProgress(ioProgress.summary, s))
                                  .getOrElse(-1.0)

    val progressValue = ioProgress.transferState.map(ts => ts.progress)
                                  .getOrElse(totalProgressValue)

    progressBar.progress = progressValue
    progressLabel.text = f"${progressValue * 100}%.1f%%"

    totalProgressIndicator.progress = totalProgressValue
  }

  private def updateValue(progress: IOProgress): Unit = {
    fileCountLabel.text = progress.summary.numFiles.toString
    dirCountLabel.text = progress.summary.numDirs.toString
    sizeLabel.text = DataUnit.formatDataSize(progress.summary.totalSize)

    progress.curMessage.foreach(msg => detailsLabel.text = msg)
  }

  private def updateTimes(workerService: Option[BackgroundService[IOProgress]]): Unit = {
    val millis = System.currentTimeMillis() - startTime
    val interval = TimeInterval(millis)
    elapsedTimeLabel.text = interval.format()
    speedLabel.text =
        workerService.map { service =>
          val speed = service.value.value.summary.totalSize / (millis / 1000.0)
          s"${DataUnit.formatDataSize(speed)}/s"
        }.getOrElse("n/a")

    estimatedTimeLabel.text =
        workerService.flatMap { service =>
          val speed = service.value.value.summary.totalSize / millis.toDouble
          val ioProgress = service.value.value
          ioProgress.jobStats.map(_.size - ioProgress.summary.totalSize)
                    .map(r => TimeInterval((r / speed).toLong).format())
        }.getOrElse("n/a")
  }
}
