package org.mikesajak.commander.task

import java.nio.channels.Channels

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.util.IO
import org.mikesajak.commander.util.Utils.runWithTimer
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType}

object CopyFileTask {
  // FixME: use configuration, not constants
  val BUFFER_SIZE: Int = 1024 * 100
}

case class CopyJobDef(source: VPath, target: VPath)

class RecursiveCopyTask(jobDefs: Seq[CopyJobDef], jobStats: Option[DirStats], dryRun: Boolean)
    extends jfxc.Task[IOProgress] {

  private implicit val logger: Logger = Logger[RecursiveCopyTask]

  private var overwriteDecision: Option[Boolean] = None

  override def call(): IOProgress = {
    runWithTimer(s"Copy filesTask: $jobDefs")(runCopy)
  }

  private def runCopy(): IOProgress = {
    val result = jobDefs.foldLeft(IOTaskSummary.empty) { case (summary, job) => copy(job, summary) }

    logger.debug(s"Finished copy task, result=$result")

    reportProgress(result)
  }

  private def copy(jobDef: CopyJobDef, summary: IOTaskSummary): IOTaskSummary = {
    val source = jobDef.source
    val target = jobDef.target
    if (source.isDirectory) copyDir(source.directory, target.directory, summary)
    else copyFile(source.asInstanceOf[VFile], target, summary)
  }

  private def copyDir(source: VDirectory, target: VDirectory, summary: IOTaskSummary): IOTaskSummary = {
    reportProgress(summary, source)

    val targetDir = if (!dryRun) target.mkChildDir(source.name)
                    else target // todo: maybe create path for this dir without actually creating directory on FS

    val dirSummary = source.childDirs.foldLeft(summary)((result, childDir) => copyDir(childDir, targetDir, result))
    val totalSummary = source.childFiles.foldLeft(dirSummary)((result, childFile) => copyFile(childFile, targetDir, result))

    val resultSummary = summary + IOTaskSummary.success(source)
    reportProgress(resultSummary, source)
    resultSummary
  }

  private def copyFile(source: VFile, target: VPath, summary: IOTaskSummary): IOTaskSummary = {
    reportProgress(summary, source)

    val targetFs = target.fileSystem

    val targetFile = if (!target.isDirectory) target.asInstanceOf[VFile]
                     else target.directory.mkChildFile(source.name)

    val doCopy = overwriteDecision.getOrElse {
      if (targetFs.exists(targetFile)) {
        logger.warn(s"Target file $targetFile exists. Overwriting!!!")
        // TODO: ask if user wants to overwrite existing file!!
        val yesToAllButtonType = new ButtonType("Yes to all")
        val noToAllButtonType = new ButtonType("No to all")
        val alert = new Alert(AlertType.Confirmation) {
          initOwner(null)
          title = "Confirm overwrite"
          headerText = "Target file already exists"
          contentText = s"Are you sure to overwrite ${targetFile.absolutePath}"
          buttonTypes = Seq(ButtonType.Yes, yesToAllButtonType, ButtonType.No, noToAllButtonType, ButtonType.Cancel)
        }

        alert.showAndWait() match {
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
      targetFs.create(targetFile)
      copyFileData(source, targetFile, summary)
    }

    val resultSummary = summary + IOTaskSummary.success(targetFile)
    reportProgress(resultSummary, targetFile)
    resultSummary
  }

  def copyFileData(source: VFile, target: VFile, curSummary: IOTaskSummary): Unit = {
    val inChannel = Channels.newChannel(source.inStream)
    val outChannel = Channels.newChannel(target.outStream)

    IO.channelCopy(inChannel, outChannel, CopyFileTask.BUFFER_SIZE, new FileCopyListener(source, curSummary))

    inChannel.close()
    outChannel.close()
  }

  class FileCopyListener(file: VFile, currentSummary: IOTaskSummary) extends IO.CopyListener {
    override def bytesWritten(size: Int): Boolean = {
      reportProgress(TransferState(size, file.size), currentSummary, file)
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
}


