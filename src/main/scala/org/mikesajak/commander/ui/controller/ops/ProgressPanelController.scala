package org.mikesajak.commander.ui.controller.ops

import java.util.{Timer, TimerTask}

import com.typesafe.scalalogging.Logger
import javafx.scene.control
import org.mikesajak.commander.task.{BackgroundService, IOProgress, IOTaskSummary}
import org.mikesajak.commander.units.{DataUnit, TimeInterval}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafxml.core.macros.sfxml

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
                                  fileCountLabel: Label,
                                  dirCountLabel: Label,
                                  sizeLabel: Label,
                                  elapsedTimeLabel: Label,
                                  estimatedTimeLabel: Label,
                                  speedLabel: Label,
                                  dontCloseCheckbox: CheckBox)
    extends ProgressPanelController {

  private val logger = Logger[ProgressPanelController]

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

    val cancelButton = getButton(ButtonType.Cancel)
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

    val closeButton = getButton(ButtonType.Close)
    if (!dontCloseCheckbox.isSelected) {
      closeButton.fire()
    }
  }

  private def getButton(buttonType: ButtonType) =
    new Button(dialog.getDialogPane.lookupButton(buttonType).asInstanceOf[control.Button])

  private def updateProgress(progress: IOProgress): Unit = {
    val progressValue = progress.jobStats.map(s => IOProgress.calcProgress(progress.summary, s))
                                .getOrElse(-1.0)

    progressBar.progress = progress.transferState.map(ts => ts.bytesDone.toDouble / ts.totalBytes)
                                   .getOrElse(progressValue)

    totalProgressIndicator.progress = progressValue
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
