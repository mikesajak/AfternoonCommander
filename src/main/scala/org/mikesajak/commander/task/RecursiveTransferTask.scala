package org.mikesajak.commander.task

import java.nio.channels.Channels

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs._
import org.mikesajak.commander.task.OperationType.{Copy, Move}
import org.mikesajak.commander.util.IO
import org.mikesajak.commander.util.Utils.runWithTimer
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

import scala.util.{Failure, Success}

object RecursiveTransferFileTask {
  // FixME: use configuration, not constants
  val BUFFER_SIZE: Int = 1024 * 1000
}

class RecursiveTransferTask(transferJob: TransferJob, jobStats: Option[DirStats], dryRun: Boolean)
    extends jfxc.Task[IOProgress] {

  private implicit val logger: Logger = Logger[RecursiveTransferTask]

  private var overwriteDecision: Option[Boolean] = None

  override def call(): IOProgress = {
    runWithTimer(s"Transfer files task: $transferJob")(runTransfer)
  }

  private def runTransfer(): IOProgress = {
    val result = transferJob.pathsToCopy.foldLeft(IOTaskSummary.empty) {
      case (summary, job) => transfer(job, summary, transferJob.operationType)
    }

    logger.debug(s"Finished copy task, result=$result")

    reportProgress(result)
  }

  private def transfer(jobDef: TransferDef, summary: IOTaskSummary, opType: OperationType): IOTaskSummary = {
    if (jobDef.source.isDirectory)
      transferDir(jobDef.source.directory, jobDef.target.directory, summary, opType)
    else
      transferFile(jobDef.source.asInstanceOf[VFile], jobDef.target, summary, opType)
  }

  private def transferDir(source: VDirectory, target: VDirectory, summary: IOTaskSummary, opType: OperationType): IOTaskSummary = {
    reportProgress(summary, source)

    val targetDir = copyDir(source, target)

    val dirSummary = source.childDirs.foldLeft(summary)((result, childDir) =>
                                                          transferDir(childDir, targetDir, result, opType))
    val totalSummary = source.childFiles.foldLeft(dirSummary)((result, childFile) =>
                                                                transferFile(childFile, targetDir, result, opType))

    val resultSummary = totalSummary + IOTaskSummary.success(source)
    reportProgress(resultSummary, source)
    resultSummary
  }

  private def copyDir(source: VDirectory, target: VDirectory) = {
    if (dryRun) target // todo: maybe create path for this dir without actually creating directory on FS
    else target.updater
               .map(updater => mkDir(updater, source))
               // todo: ask user if continue instead of cancelling whole copy operation
               .getOrElse(throw new CopyException(
                 s"Target directory $target is not writable. Cannot create child directory ${source.name}."))
  }

  private def mkDir(parentUpdater: VDirectoryUpdater, source: VDirectory) = {
    val childDir = parentUpdater.mkChildDir(source.name)
    if (transferJob.preserveModificationDate)
      childDir.updater.foreach(_.setModificationDate(source.modificationDate))
    childDir
  }

  private def transferFile(source: VFile, target: VPath, summary: IOTaskSummary, opType: OperationType): IOTaskSummary =
    opType match {
      case Copy => copyFile(source, target, summary)
      case Move => moveFile(source, target, summary)
    }

  private def moveFile(source: VFile, target: VPath, summary: IOTaskSummary): IOTaskSummary = {
    reportProgress(summary, source)
    source.updater.map { updater =>
      updater.move(target.directory, Some(target.name)) match {
        case Success(true) =>
          summary + IOTaskSummary.success(source)
        case Success(false) =>
          copyFile(source, target, summary)
        case Failure(exception) =>
          summary + IOTaskSummary.failed(source, s"Move operation failed. ${exception.getLocalizedMessage}")
      }
    }.getOrElse(IOTaskSummary.failed(source, s"Move operation failed. Target location $target is not writable."))
  }

  private def copyFile(source: VFile, target: VPath, summary: IOTaskSummary): IOTaskSummary = {
    reportProgress(summary, source)

    val targetFile =
      if (!target.isDirectory) target.asInstanceOf[VFile]
      else target.directory.updater
                 .map(_.mkChildFile(source.name))
                 // todo: ask user if continue instead of cancelling whole copy operation
                 .getOrElse(throw new CopyException(
                   s"Target directory ${target.directory} is not writable. Cannot create child file ${source.name}"))

    val doCopy = overwriteDecision.getOrElse {
      // TODO: refactor this!
      if (targetFile.exists) {
         confirmOverwriteDialog(targetFile) match {
          case Some(ButtonType.Yes) =>
            logger.debug("selected Yen")
            true
          case Some(yesToAllButtonType) =>
            logger.debug("selected Yes to all")
            overwriteDecision = Some(true)
            true
          case Some(ButtonType.No) =>
            logger.debug("selected No")
            false
          case Some(noToAllButtonType) =>
            logger.debug("selected No to all")
            overwriteDecision = Some(false)
            false
          case Some(ButtonType.Cancel) =>
            logger.debug("selected Cancel")
            throw new CancelledException()
          case _ => false
        }
      } else true
    }

    if (doCopy && !dryRun) {
      targetFile.updater.foreach { updater =>
        if (!targetFile.exists)
          updater.create()
        copyFileData(source, updater, summary)
        if (transferJob.preserveModificationDate)
          updater.setModificationDate(source.modificationDate)
      }
    }

    val resultSummary = summary + IOTaskSummary.success(targetFile)
    reportProgress(resultSummary, targetFile)
    resultSummary
  }

  private def confirmOverwriteDialog(targetFile: VPath) = {
    val yesToAllButtonType = new ButtonType("Yes to all")
    val noToAllButtonType = new ButtonType("No to all")
    val alert = new Alert(AlertType.Confirmation) {
      initOwner(null)
      title = "Confirm overwrite"
      headerText = "Target file already exists"
      contentText = s"Are you sure to overwrite ${targetFile.absolutePath}"
      buttonTypes = Seq(ButtonType.Yes, yesToAllButtonType, ButtonType.No, noToAllButtonType, ButtonType.Cancel)
    }

    alert.showAndWait()
  }

  def copyFileData(source: VFile, target: VFileUpdater, curSummary: IOTaskSummary): Unit = {
    val inChannel = Channels.newChannel(source.inStream)
    val outChannel = Channels.newChannel(target.outStream)
    try {
      IO.channelCopy(inChannel, outChannel, RecursiveTransferFileTask.BUFFER_SIZE, new FileCopyListener(source, curSummary))
    } finally {
      inChannel.close()
      outChannel.close()
    }
  }

  class FileCopyListener(file: VFile, currentSummary: IOTaskSummary) extends IO.CopyListener {
    override def notifyBytesWritten(size: Long): Boolean = {
      val updatedSummary = IOTaskSummary(currentSummary.numDirs, currentSummary.numFiles,
                                         currentSummary.totalSize + size, currentSummary.errors)
      reportProgress(TransferState(size, file.size), updatedSummary, file)
      isCancelled
    }
  }

  private def reportProgress(transferState: TransferState, summary: IOTaskSummary, curPath: VPath): IOProgress = {
    val progress = IOProgress(Some(transferState), summary, Some(curPath.absolutePath), jobStats)
    updateValue(progress)
    progress
  }

  private def reportProgress(summary: IOTaskSummary, curPath: VPath): IOProgress = {
    val progress = IOProgress(TransferState(1L, 1L), summary, Some(curPath.absolutePath), jobStats)
    updateValue(progress)
    progress
  }

  private def reportProgress(summary: IOTaskSummary): IOProgress = {
    val progress = IOProgress(TransferState(0L, 1L), summary, None, jobStats)
    updateValue(progress)
    progress
  }

  class CopyException(msg: String) extends Exception(msg)
}


