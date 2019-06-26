package org.mikesajak.commander.task

import java.nio.channels.Channels

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.util.IO
import org.mikesajak.commander.util.Utils.runWithTimer

object CopyFileTask {
  // FixME: use configuration, not constants
  val BUFFER_SIZE: Int = 1024 * 100
}

case class CopyJobDef(source: VPath, target: VPath)

class RecursiveCopyTask(jobDefs: Seq[CopyJobDef], jobStats: Option[DirStats], dryRun: Boolean)
    extends jfxc.Task[IOProgress] {

  private implicit val logger = Logger[RecursiveCopyTask]

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

    if (targetFs.exists(targetFile))
      logger.warn(s"Target file $targetFile exists. Overwriting!!!")
      // TODO: ask if user wants to overwrite existing file!!
    else
      if (!dryRun) {
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


