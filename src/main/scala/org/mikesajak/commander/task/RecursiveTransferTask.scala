package org.mikesajak.commander.task

import java.util.concurrent.FutureTask

import com.typesafe.scalalogging.Logger
import enumeratum.{Enum, EnumEntry}
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import org.mikesajak.commander.fs._
import org.mikesajak.commander.task.Decision.{No, Yes}
import org.mikesajak.commander.task.OperationType.{Copy, Move}
import org.mikesajak.commander.task.OverwriteDecision.{NoToAll, YesToAll}
import org.mikesajak.commander.util.IO
import org.mikesajak.commander.util.Utils.runWithTimer
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

class RecursiveTransferTask(transferJob: TransferJob, jobStats: Option[DirStats],
                            dryRun: Boolean, config: Configuration)
    extends jfxc.Task[IOProgress] {

  private implicit val logger: Logger = Logger[RecursiveTransferTask]

  private var overwriteDecision: Option[OverwriteDecision] = None

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
    try {
      if (jobDef.source.isDirectory)
        transferDir(jobDef.source.directory, jobDef.target, summary, opType)
      else
        transferFile(jobDef.source.asInstanceOf[VFile], jobDef.target, summary, opType)
    } catch {
      case e: TransferException =>
        logger.info(s"An error occurred during $opType operation. Detailed error: ${e.getLocalizedMessage}", e)
        showErrorDialogAndAskForDecision(opType, e.getLocalizedMessage) match {
          case ContinueAfterErrorButtonTypes.Retry =>
            logger.info(s"User selected retry.")
            transfer(jobDef, summary, opType)
          case ContinueAfterErrorButtonTypes.Skip => logger.info(s"User selected skip.")
          case ContinueAfterErrorButtonTypes.Abort =>
            logger.info(s"User selected abort after an error occurred during $opType operation. Detailed error: ${e.getLocalizedMessage}")
            throw e
        }
        summary + IOTaskSummary.failed(jobDef.source, e.getLocalizedMessage)
    }
  }

  private def transferDir(source: VDirectory, target: VPath, summary: IOTaskSummary, opType: OperationType): IOTaskSummary = {
    reportProgress(summary, source)

    val targetDir =
      if (target.isDirectory) target.directory
      else throw new TransferException(s"Cannot copy directory $source, target path $target is existing file.")

    val resultDir = copyDir(source, targetDir)

    val dirSummary = source.childDirs.foldLeft(summary)((result, childDir) =>
                                                          transferDir(childDir, resultDir, result, opType))
    val totalSummary = source.childFiles.foldLeft(dirSummary)((result, childFile) =>
                                                                transferFile(childFile, resultDir, result, opType))

    val resultSummary = totalSummary + IOTaskSummary.success(source)
    reportProgress(resultSummary, source)
    resultSummary
  }

  private def copyDir(source: VDirectory, target: VDirectory) = {
    if (dryRun) target // todo: maybe create path for this dir without actually creating directory on FS
    else target.updater
               .map(updater => mkDir(updater, source, transferJob.preserveModificationDate))
               // todo: ask user if continue instead of cancelling whole copy operation
               .getOrElse(throw new TransferException(
                 s"Target directory $target is not writable. Cannot create child directory ${source.name}."))
  }

  private def transferFile(source: VFile, target: VPath, summary: IOTaskSummary, opType: OperationType): IOTaskSummary = {
    reportProgress(summary, source)

    val resultSummary = {
      opType match {
        case Copy => copyFile(source, target, summary)
        case Move => moveFile(source, target, summary)
      }
      summary + IOTaskSummary.success(target)
    }

    reportProgress(resultSummary, target)
    resultSummary
  }

  private def moveFile(source: VFile, target: VPath, summary: IOTaskSummary): Unit = {
    val targetFile =
      if (!target.isDirectory) target.asInstanceOf[VFile]
      else target.directory.updater
                 .map(_.mkChildFilePath(source.name))
                 // todo: ask user if continue instead of cancelling whole copy operation
                 .getOrElse(throw new TransferException(
                   s"Target directory ${target.directory} is not writable. Cannot create child file ${source.name}"))

    val proceedWithMove = if (!targetFile.exists) Yes
                 else overwriteDecision.getOrElse(getOverwriteDecision(targetFile))

    if (proceedWithMove == Yes && !dryRun) {
      // first try to rename
      val renameSucceeded = source.updater.map(_.move(target.directory, Some(target.name))) match {
        case Some(Success(renamed)) => renamed
        case Some(Failure(cause)) =>
          throw new TransferException(s"Move operation failed. ${cause.getLocalizedMessage}", cause)
        case None => throw new TransferException(s"Move operation failed. Source file $source cannot be modified.") // TODO: i18
      }

      if (!renameSucceeded) {
        // if quick rename/move is not possible, then fallback to copy+delete
        doCopy(source, targetFile, transferJob.preserveModificationDate, new FileCopyListener(source, summary))
        doDelete(source)
      }
    }
  }

  private def copyFile(source: VFile, target: VPath, summary: IOTaskSummary): Unit /*: IOTaskSummary*/ = {
    val targetFilePath =
      if (!target.isDirectory) target.asInstanceOf[VFile]
      else target.directory.updater
                 .map(_.mkChildFilePath(source.name))
                 // todo: ask user if continue instead of cancelling whole copy operation
                 .getOrElse(throw new TransferException(
                   s"Target directory ${target.directory} is not writable. Cannot create child file ${source.name}"))

    val proceedWithCopy = if (!targetFilePath.exists) Yes
                 else overwriteDecision.getOrElse(getOverwriteDecision(targetFilePath))

    if (proceedWithCopy == Yes && !dryRun)
      doCopy(source, targetFilePath, transferJob.preserveModificationDate, new FileCopyListener(source, summary))
  }


  private def getOverwriteDecision(targetFile: VPath): Decision = {
    overwriteDecision match {
      case Some(YesToAll) => Yes
      case Some(NoToAll) => No
      case None =>
        showConfirmOverwriteDialog(targetFile) match {
          case Some(DecisionButtonTypes.Yes) => Yes
          case Some(DecisionButtonTypes.No) => No
          case Some(DecisionButtonTypes.YesToAll) =>
            overwriteDecision = Some(YesToAll)
            Yes
          case Some(DecisionButtonTypes.NoToAll) =>
            overwriteDecision = Some(NoToAll)
            No
          case Some(DecisionButtonTypes.Cancel) | None =>
            throw new CancelledException() // TODO: value
        }
    }
  }

  object DecisionButtonTypes {
    val Yes: ButtonType = ButtonType.Yes
    val No: ButtonType = ButtonType.No
    val YesToAll: ButtonType = new ButtonType("Yes to all")
    val NoToAll: ButtonType = new ButtonType("No to all")
    val Cancel: ButtonType = ButtonType.Cancel
  }

  private def showConfirmOverwriteDialog(targetFile: VPath): Option[ButtonType] = {
    val futureTask = new FutureTask(() =>
      new Alert(AlertType.Confirmation) {
        initOwner(null)
        title = "Confirm overwrite"
        headerText = "Target file already exists"
        contentText = s"Are you sure to overwrite ${targetFile.absolutePath}"
        buttonTypes = Seq(DecisionButtonTypes.Yes, DecisionButtonTypes.YesToAll,
                          DecisionButtonTypes.No, DecisionButtonTypes.NoToAll,
                          DecisionButtonTypes.Cancel)
      }.showAndWait())

    Platform.runLater(futureTask)
    futureTask.get()
  }

  object ContinueAfterErrorButtonTypes {
    val Retry: ButtonType = new ButtonType("Retry")
    val Skip: ButtonType = new ButtonType("Skip")
    val Abort: ButtonType = new ButtonType("Abort")
  }

  private def showErrorDialogAndAskForDecision(opType: OperationType, errorMsg: String): Option[ButtonType]  = {
    val futureTask = new FutureTask(() =>
      new Alert(AlertType.Confirmation) {
        initOwner(null)
        title = "Operation failed, continue?"
        headerText = s"An error occurred during $opType operation. Do you want to continue?"
        contentText = s"$errorMsg"
        buttonTypes = Seq(ContinueAfterErrorButtonTypes.Retry, ContinueAfterErrorButtonTypes.Skip,
                          ContinueAfterErrorButtonTypes.Abort)
      }.showAndWait())
    Platform.runLater(futureTask)
    futureTask.get()
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


  def mkDir(parentUpdater: VDirectoryUpdater, source: VDirectory, preserveModificationDate: Boolean = false): VDirectory = {
    val childDir = parentUpdater.mkChildDirPath(source.name)
    val maybeTriedBoolean: Option[Try[Boolean]] = childDir.updater.map(_.create())
    maybeTriedBoolean match {
      case Some(Success(true)) =>
        if (preserveModificationDate)
          childDir.updater.foreach(_.setModificationDate(source.modificationDate)) // TODO: add warning to collector
        childDir
      case Some(Success(false)) => throw new TransferException(s"Creating directory $childDir failed.")
      case Some(Failure(cause)) =>
        throw new TransferException(s"Creating directory $childDir failed. ${cause.getLocalizedMessage}", cause)
      case None => throw new TransferException(s"Couldn't create directory $childDir, parent directory is not writable")
    }
  }

  def doDelete(source: VFile): Unit = {
    source.updater.map(_.delete()) match {
      case Some(Success(true)) =>
      case Some(Success(false)) => throw new TransferException(s"Deleting file $source failed.")
      case Some(Failure(cause)) =>
        throw new TransferException(s"Deleting file $source failed. ${cause.getLocalizedMessage}", cause)
      case None => throw new TransferException(s"Couldn't delete file $source, it is not writable.")
    }
  }

  def doCopy(source: VFile, targetFile: VFile, preserveModificationDate: Boolean, copyListener: IO.CopyListener): Unit = {
    targetFile.updater.map { updater =>
      if (!targetFile.exists)
        updater.create()
      val bufferSize = config.intProperty(ConfigKeys.TransferBufferSize).getOrElse(1024 * 1000)
      IO.bufferedCopy(source.inStream, updater.outStream, bufferSize, copyListener)
      if (preserveModificationDate)
        updater.setModificationDate(source.modificationDate) // TODO: add warning to collector
    }.getOrElse(throw new TransferException(s"Couldn't copy file $source. Target file $targetFile is not writable"))
  }

  class TransferException(msg: String) extends Exception(msg) {
    def this(msg: String, cause: Throwable) = {
      this(msg)
      initCause(cause)
    }
  }

  class FileCopyListener(file: VFile, currentSummary: IOTaskSummary) extends IO.CopyListener {
    override def notifyBytesWritten(size: Long): Unit = {
      val updatedSummary = IOTaskSummary(currentSummary.numDirs, currentSummary.numFiles,
                                         currentSummary.totalSize + size, currentSummary.errors)
      reportProgress(TransferState(size, file.size), updatedSummary, file)

      if (isCancelled) {
        throw CancelledException(updatedSummary)
      }
    }
  }
}

sealed trait OverwriteDecision extends EnumEntry
object OverwriteDecision extends Enum[OverwriteDecision] {
  override val values: immutable.IndexedSeq[OverwriteDecision] = findValues
  case object YesToAll extends OverwriteDecision
  case object NoToAll extends OverwriteDecision
}

sealed trait Decision extends EnumEntry
object Decision extends Enum[Decision] {
  override val values: immutable.IndexedSeq[Decision] = findValues

  case object Yes extends Decision
  case object No extends Decision
}
