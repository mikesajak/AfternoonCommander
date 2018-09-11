package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.task.CancellableTask._
import org.mikesajak.commander.util.Utils._

import scala.util.{Failure, Success}

class RecursiveDeleteTask(targetPaths: Seq[VPath], targetStats: Option[DirStats]) extends Task[IOTaskSummary] with CancellableTask {
  private implicit val logger: Logger = Logger[RecursiveDeleteTask]

  private var progressMonitor: ProgressMonitor[IOTaskSummary] = _

  override def run(progressMonitor: ProgressMonitor[IOTaskSummary]): Option[IOTaskSummary] = {
    this.progressMonitor = progressMonitor

    val targetName = targetPaths match {
      case p: Seq[VPath] if p.size == 1 => p.toString
      case p @ _ => s"${p.size} elements" // TODO: i18
    }

    withAbort(progressMonitor) { () =>
      val result = runWithTimer(s"Recursive task : delete $targetName") { () =>
        progressMonitor.notifyProgress(0, Some(s"Starting recursive delete of $targetName"), Some(IOTaskSummary.empty)) // TODO: i18

        targetPaths.map(p => deletePath(p, IOTaskSummary.empty))
                   .reduceLeft((acc, stats) => acc + stats)
      }

      if (result.errors.isEmpty) progressMonitor.notifyFinished(Some("Finished delete of $path"), Some(result)) // TODO: i18
      else progressMonitor.notifyError(s"Delete of $targetName finished with errors: ${result.errors}", Some(result)) // TODO: i18

      result
    }
  }

  private def deletePath(path: VPath, curSummary: IOTaskSummary): IOTaskSummary = {
    abortIfNeeded()

    val result = if (path.isDirectory) deleteDir(path.asInstanceOf[VDirectory], curSummary)
                 else deleteFile(path.asInstanceOf[VFile], curSummary)

    updateProgress(s"$path", result, progressMonitor)
    result
  }

  private def deleteFile(file: VFile, curSummary: IOTaskSummary): IOTaskSummary = {
    val fs = file.fileSystem
    val result = fs.delete(file) match {
        case Success(deleted) =>
          if (deleted) IOTaskSummary.success(file)
          else IOTaskSummary.empty

        case Failure(exception) => IOTaskSummary.failed(file, s"Delete of file $file failed with error: $exception")
      }
    result + curSummary
  }

  private def deleteDir(dir: VDirectory, curSummary: IOTaskSummary): IOTaskSummary = {
    val childSummaryResult = (dir.children foldLeft curSummary) { (accSummary, child) => deletePath(child, accSummary) }

    val result = if (childSummaryResult.isSuccessful) {
      val fs = dir.fileSystem
      fs.delete(dir) match {
        case Success(deleted) => if (deleted) IOTaskSummary.success(dir)
                                 else IOTaskSummary.failed(dir, s"Directory $dir, cannot be deleted. Dir contents: ${dir.children}")

        case Failure(exception) => IOTaskSummary.failed(dir, s"Delete of directory $dir failed with error: $exception")
      }
    } else IOTaskSummary.failed(dir, s"Can't delete directory $dir because of previous errors")

    result + childSummaryResult
  }

  private def updateProgress(message: String, summary: IOTaskSummary, progressMonitor: ProgressMonitor[IOTaskSummary]): Unit = {
    targetStats match {
      case Some(stats) => progressMonitor.notifyProgress(countProgress(stats, summary), Some(message), Some(summary))
      case None => progressMonitor.notifyProgressIndeterminate(Some(message), Some(summary))
    }
  }

  private def countProgress(target: DirStats, current: IOTaskSummary) = {
    val targetCount = target.numFiles + target.numDirs
    val curCount = current.numFiles + current.numDirs
    val progress = curCount / targetCount.toFloat
    Math.min(progress, 1.0f)
  }

}
