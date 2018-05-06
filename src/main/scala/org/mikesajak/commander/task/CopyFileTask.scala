package org.mikesajak.commander.task

import java.nio.channels.Channels

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.task.CancellableTask._
import org.mikesajak.commander.util.IO

import scala.collection.mutable

object CopyFileTask {
  // FixME: use configuration, not constants
  val BUFFER_SIZE: Int = 1024 * 100
}

case class CopyJobDef(source: VPath, target: VPath)

class CopyMultiFilesTask(files: Seq[CopyJobDef], jobStats: Option[DirStats]) extends Task[IOTaskSummary] with CancellableTask {

  private val logger = Logger[CopyMultiFilesTask]

  override def run(progressMonitor: ProgressMonitor[IOTaskSummary]): Option[IOTaskSummary] = {

    logger.debug(s"starting task for $files, stats=$jobStats")

    withAbort(progressMonitor) { () =>

      val copyListener = new CopyProgressListenerImpl(jobStats, progressMonitor)
      val results =
        for (file <- files) yield {
          try {
            copy(file.source, file.target, copyListener)
            IOTaskSummary.success(file.source)
          } catch {
            case e: Exception => IOTaskSummary.failed(file.source, e.getLocalizedMessage)
          }
        }

      results.reduce((a, b) => a + b)
    }
  }

  def copy(source: VPath, target: VPath, copyProgressListener: CopyProgressListener): Unit =
    if (source.isDirectory)
      copyDir(source.directory, target.directory, copyProgressListener)
    else {
      copyFile(source.asInstanceOf[VFile], target, copyProgressListener)
    }

  def copyDir(source: VDirectory, target: VDirectory, copyListener: CopyProgressListener): Unit = {
    val targetDir = target.mkChildDir(source.name)
    source.childDirs.foreach(dir => copyDir(dir, targetDir, copyListener))
    source.childFiles.foreach(file => copyFile(file, targetDir, copyListener))

    copyListener.dirFinished(source)
  }

  def copyFile(source: VFile, target: VPath, copyListener: CopyProgressListener): Unit = {
    val targetFs = target.fileSystem

    val targetFile = if (!target.isDirectory) target.asInstanceOf[VFile]
                     else target.directory.mkChildFile(source.name)

    if (!targetFs.exists(targetFile))
      targetFs.create(targetFile)
    // TODO: ask if user wants to overwrite existing file!!
    else logger.warn(s"Target file $targetFile exists. Overwriting!!!")

    copyFileData(source, targetFile, copyListener)

    copyListener.fileFinished(source)
  }

  def copyFileData(source: VFile, target: VFile, copyListener: CopyProgressListener): Unit = {
    val inChannel = Channels.newChannel(source.inStream)
    val outChannel = Channels.newChannel(target.outStream)

    IO.channelCopy(inChannel, outChannel, CopyFileTask.BUFFER_SIZE, new FileCopyListener(source, copyListener))

    inChannel.close()
    outChannel.close()
  }

  class FileCopyListener(file: VFile, copyProgressListener: CopyProgressListener) extends IO.CopyListener {
    override def bytesWritten(size: Int): Boolean = {
      copyProgressListener.bytesCopied(file, size)
      true
    }
  }
}

trait CopyProgressListener {
  def bytesCopied(file: VFile, bytesCount: Long)
  def fileFinished(file: VFile)
  def dirFinished(dir: VDirectory)
}

class CopyProgressListenerImpl(jobStats: Option[DirStats], progressMonitor: ProgressMonitor[IOTaskSummary]) extends CopyProgressListener {
  private val logger = Logger[CopyProgressListenerImpl]

  private var totalCopiedSize = 0L
  private var progressMap = mutable.Map[VFile, Long]()
  private var totalCopiedFiles = 0
  private var totalCopiedDirs = 0

  override def bytesCopied(file: VFile, bytesCount: Long): Unit = {
//    logger.debug(s"bytesCopied: ${file.name} => $bytesCount")
    totalCopiedSize += bytesCount
    val fileCopiedBytes = progressMap.getOrElseUpdate(file, 0)
    progressMap(file) = fileCopiedBytes + bytesCount
    notifyProgress(Some(file))
  }

  override def fileFinished(file: VFile): Unit = {
    totalCopiedFiles += 1
    notifyProgress(Some(file))
    progressMap -= file
  }

  override def dirFinished(dir: VDirectory): Unit = {
    totalCopiedDirs += 1
    notifyProgress(None)
  }

  private def notifyProgress(file: Option[VFile]): Unit = {
    jobStats match {
      case None =>
        progressMonitor.notifyProgressIndeterminate(None,
          Some(IOTaskSummary(totalCopiedDirs, totalCopiedFiles, totalCopiedSize, List.empty)))

      case Some(stats) if stats.numFiles == 1 =>
        file match {
          case Some(f) =>
            progressMonitor.notifyProgress(progressMap(f).toFloat / f.size, None,
              Some(IOTaskSummary(totalCopiedDirs, totalCopiedFiles, totalCopiedSize, List.empty)))
          case _ =>
            // skip, cannot happen
            logger.debug(s"This cannot happen. notifyProgress(file: $file)")
        }

      case Some(stats) =>
        val partProgress = file.map(f => progressMap(f).toFloat / f.size).getOrElse(0f)
        progressMonitor.notifyDetailedProgress(partProgress, totalCopiedSize.toFloat / stats.size, None,
          Some(IOTaskSummary(totalCopiedDirs, totalCopiedFiles, totalCopiedSize, List.empty)))
    }
  }
}

