package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import enumeratum.{Enum, EnumEntry}
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import org.mikesajak.commander.fs._
import org.mikesajak.commander.task.Decision.{No, Yes}
import org.mikesajak.commander.task.OperationType.{Copy, Move}
import org.mikesajak.commander.task.OverwriteDecision.{NoToAll, YesToAll}
import org.mikesajak.commander.util.IO
import org.mikesajak.commander.util.Utils._

import scala.collection.immutable
import scala.util.{Failure, Success}

class RecursiveTransferTask(transferJob: TransferJob, jobStats: Option[DirStats],
                            dryRun: Boolean, config: Configuration,
                            userDecisionCtrl: UserDecisionCtrl)
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
        userDecisionCtrl.showErrorDialogAndAskForDecision("Operation failed, continue?",
                                                          s"An error occurred during $opType operation. Do you want to continue?",
                                                          e.getLocalizedMessage) match {
          case Some(userDecisionCtrl.RetryButtonType) =>
            logger.info(s"User selected retry.")
            transfer(jobDef, summary, opType)
          case Some(userDecisionCtrl.SkipButtonType) => logger.info(s"User selected skip.")
          case None | Some(userDecisionCtrl.AbortButtonType) =>
            logger.info(s"User selected abort after an error occurred during $opType operation. Detailed error: ${e.getLocalizedMessage}")
            throw e
        }
        summary + IOTaskSummary.failed(jobDef.source, e.getLocalizedMessage)
    }
  }

  private def transferDir(source: VDirectory, target: VPath, summary: IOTaskSummary, opType: OperationType): IOTaskSummary = {
    checkCancelled()
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
    checkCancelled()
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
    logger.debug(s"Moving file $source -> $target")
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
        logger.trace(s"Quick rename/move $source -> $target didn't succeed, fallback to slow copy+delete")
        // if quick rename/move is not possible, then fallback to copy+delete
        doCopy(source, targetFile, transferJob.preserveModificationDate, new FileCopyListener(source, summary))
        doDelete(source)
      } else logger.trace(s"Quick rename/move $source -> $target succeeded")
    }
  }

  private def copyFile(source: VFile, target: VPath, summary: IOTaskSummary): Unit = {
    logger.debug(s"Copying file $source -> $target")
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
        userDecisionCtrl.showYesNoAllCancelDialog("Confirm overwrite",
                                                  "Target file already exists",
                                                  s"Are you sure to overwrite ${targetFile.absolutePath}") match {
          case Some(userDecisionCtrl.YesButtonType) => Yes
          case Some(userDecisionCtrl.NoButtonType) => No
          case Some(userDecisionCtrl.YesToAllButtonType) =>
            overwriteDecision = Some(YesToAll)
            Yes
          case Some(userDecisionCtrl.NoToAllButtonType) =>
            overwriteDecision = Some(NoToAll)
            No
          case Some(userDecisionCtrl.CancelButtonType) | None =>
            throw new CancelledException() // TODO: value
        }
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


  def mkDir(parentUpdater: VDirectoryUpdater, source: VDirectory, preserveModificationDate: Boolean = false): VDirectory = {
    val childDir = parentUpdater.mkChildDirPath(source.name)
    childDir.updater.map(_.create()) match {
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

  private def checkCancelled(): Unit = {
    if (isCancelled) {
      logger.debug(s"Cancel request was detected - stopping current task.")
      throw new CancelledException
    }
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

      checkCancelled()
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
