package org.mikesajak.commander.task

import java.nio.channels.Channels

import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.task.CancellableTask._
import org.mikesajak.commander.util.IO

object CopyFileTask {
  // FixME: use configuration, not constants
  val BUFFER_SIZE: Int = 1024 * 512
}

class CopyFileTask(source: VFile, target: VFile) extends Task[IOTaskSummary] with CancellableTask {
  override def run(progressMonitor: ProgressMonitor[IOTaskSummary]): Option[IOTaskSummary] = {
    val fs = target.fileSystem
    if (!fs.exists(target))
      fs.create(target)

//    if (source.isInstanceOf[LocalFile] && target.isInstanceOf[LocalFile])
//      copyLocalFiles(source.asInstanceOf[LocalFile], target.asInstanceOf[LocalFile])
//    else {
//
//      copyAsStreams(source.inStream, target.outStream)
//    }
    val result =
      withAbort(progressMonitor) { () =>
        val res =
          try {
            copyWithChannels(source, target, progressMonitor)
            IOTaskSummary.success(source)
          } catch {
            case e: Exception => IOTaskSummary.failed(source, e.getLocalizedMessage)
          }
        res
      }

    result
  }

  def copyWithChannels(source: VFile, target: VFile, progressMonitor: ProgressMonitor[IOTaskSummary]): Unit = {
    val inChannel = Channels.newChannel(source.inStream)
    val outChannel = Channels.newChannel(target.outStream)

    IO.channelCopy(inChannel, outChannel, new CopyListenerImpl(source.size, progressMonitor))

    inChannel.close()
    outChannel.close()
  }

  class CopyListenerImpl(expectedSize: Long, progressMonitor: ProgressMonitor[IOTaskSummary]) extends IO.CopyListener {
    private var accumulatedSize = 0L
    override def bytesWritten(size: Int): Boolean = {
      abortIfNeeded()
      accumulatedSize += size
      progressMonitor.notifyProgress(accumulatedSize.toFloat / expectedSize, None, Some(IOTaskSummary(0, 0, accumulatedSize, List())))
      true
    }
  }
}

case class CopyJobDef(source: VPath, target: VPath)

class CopyMultiFilesTask(files: Seq[CopyJobDef], dirStats: Option[DirStats]) extends Task[IOTaskSummary] with CancellableTask {
  override def run(progressMonitor: ProgressMonitor[IOTaskSummary]): Option[IOTaskSummary] = {
    withAbort(progressMonitor) { () =>
      val copyListener = new MultiCopyProgressListener(dirStats, progressMonitor)

      val results =
        for (file <- files) yield {
          try {
            copyListener.nextFile(file.source.size)
            copy(file.source, file.target, copyListener)
            IOTaskSummary.success(file.source)
          } catch {
            case e: Exception => IOTaskSummary.failed(file.source, e.getLocalizedMessage)
          }
        }

      results.reduce((a, b) => a + b)
    }
  }

  def copy(source: VPath, target: VPath, copyProgressListener: MultiCopyProgressListener): Unit =
    if (source.isDirectory)
      copyDir(source.directory, target.directory, copyProgressListener)
    else {
      copyFile(source.asInstanceOf[VFile], target, copyProgressListener)
    }

  def copyDir(source: VDirectory, target: VDirectory, copyListener: MultiCopyProgressListener): Unit = ???

  def copyFile(source: VFile, target: VPath, copyListener: MultiCopyProgressListener): Unit = {
    val targetFs = target.fileSystem

    val targetFile = if (!target.isDirectory) target.asInstanceOf[VFile]
                     else target.directory.mkChildFile(source.name)

    if (!targetFs.exists(targetFile))
      targetFs.create(targetFile)

    copyFileData(source, targetFile, copyListener)
  }

  def copyFileData(source: VFile, target: VFile, copyListener: MultiCopyProgressListener): Unit = {
    val inChannel = Channels.newChannel(source.inStream)
    val outChannel = Channels.newChannel(target.outStream)

    IO.channelCopy(inChannel, outChannel, copyListener)

    inChannel.close()
    outChannel.close()
  }

  class MultiCopyProgressListener(dirStats: Option[DirStats], progressMonitor: ProgressMonitor[IOTaskSummary]) extends IO.CopyListener {
    private var fileCount = 0
    private var dirCount = 0
    private var curFileSize = 0L
    private var curFileExpectedSize = 0L
    private var accumulatedSize = 0L

    def nextFile(expectedSize: Long): Unit = {
      fileCount += 1
      curFileExpectedSize = expectedSize
      curFileSize = 0
    }

    def nextDir(): Unit = {
      dirCount += 1
    }

    override def bytesWritten(size: Int): Boolean = {
      abortIfNeeded()
      curFileSize += size
      accumulatedSize += size

      dirStats match {
        case None =>
          progressMonitor.notifyProgressIndeterminate(None, Some(IOTaskSummary(0, fileCount, accumulatedSize, List.empty)))

        case Some(stats @ DirStats(numFiles, _, _, _)) if numFiles == 1 =>
          progressMonitor.notifyProgress(accumulatedSize.toFloat / stats.size, None, Some(IOTaskSummary(0, 0, accumulatedSize, List.empty)))

        case _ @ stats =>
          progressMonitor.notifyDetailedProgress(curFileSize.toFloat / curFileExpectedSize,
            accumulatedSize.toFloat / dirStats.size, None, Some(IOTaskSummary(0, fileCount, accumulatedSize, List.empty)))
      }

      Thread.sleep(100)

      true
    }
  }
}

