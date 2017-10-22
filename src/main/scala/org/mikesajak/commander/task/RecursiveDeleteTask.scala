package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.task.ProgressMonitor._
import org.mikesajak.commander.util.Utils._


class RecursiveDeleteTask(path: VPath) extends Task[IOTaskSummary] {
  private val logger = Logger[RecursiveDeleteTask]

  override def run(progressMonitor: ProgressMonitor[IOTaskSummary]): Unit = {
    val result = runWithTimer(s"Recursive task : delete $path") { () =>
      progressMonitor.notifyProgress(0, Some(s"Starning recursive delete of $path"), None)
      deletePath(path)
    }

    if (result.errors.isEmpty) progressMonitor.notifyFinished("Finished delete of $path", Some(result))
    else progressMonitor.notifyError(s"Delete of $path finished with errors: ${result.errors}", Some(result))
  }

  private def deletePath(path: VPath)(implicit progressMonitor: ProgressMonitor[IOTaskSummary]): IOTaskSummary = {
    progressMonitor.notifyProgressIndeterminate(Some(s"Start deleting directory $path"), None)

    val result = if (path.isDirectory) deleteDir(path.asInstanceOf[VDirectory])
                 else deleteFile(path.asInstanceOf[VFile])

    progressMonitor.notifyProgressIndeterminate(Some(s"Finish: Deleting directory $path"), None)
    result
  }

  private def deleteFile(file: VFile)(implicit progressMonitor: ProgressMonitor[IOTaskSummary]): IOTaskSummary =
    runWithProgress(s"Deleting file $file") { () =>
      val fs = file.fileSystem
      if (fs.delete(file)) IOTaskSummary.success(file)
      else IOTaskSummary.failed(file, s"Can't delete file: $file")
    }

  private def deleteDir(dir: VDirectory)(implicit progressMonitor: ProgressMonitor[IOTaskSummary]): IOTaskSummary =
    runWithProgress("Deleting directory $dir") { () =>
      val results =
        for (child <- dir.children) yield deletePath(child)

      val result = (dir.children foldLeft IOTaskSummary(0, 0, 0, List())) { (curResult, child) =>
        val childRes = deletePath(child)
        curResult merge childRes
      }

      if (result.isSuccessful) {
        val fs = dir.fileSystem
        if (fs.delete(dir)) IOTaskSummary.success(dir)
        else IOTaskSummary.failed(dir, s"Delete direcory $dir has failed")
      } else
        IOTaskSummary.failed(dir, s"Can't delete directory $dir because of previous errors\n${result.errors}")
    }
}
