package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.util.Utils.runWithTimer

case class DeleteJobDef(target: VPath)

class RecursiveDeleteTask(jobDefs: Seq[DeleteJobDef], jobStats: Option[DirStats], dryRun: Boolean)
    extends jfxc.Task[IOProgress] {
  private implicit val logger: Logger = Logger[RecursiveDeleteTask]

  override def call(): IOProgress = {
    runWithTimer(s"Delete files task: $jobDefs")(runDelete)
  }

  private def runDelete(): IOProgress = {
    val result = jobDefs.foldLeft(IOTaskSummary.empty) { case (summary, job) => delete(job.target, summary) }

    logger.debug(s"Firished delete task, result=$result")

    reportProgress(result)
  }

  private def delete(target: VPath, summary: IOTaskSummary) = {
    if (target.isDirectory) deleteDir(target.directory, summary)
    else deleteFile(target.asInstanceOf[VFile], summary)
  }

  private def deleteFile(file: VFile, summary: IOTaskSummary): IOTaskSummary = {
//    logger.trace(s"Deleting file: $file")
    reportProgress(summary, file)
    performDelete(file)

    val resultSummary = summary + IOTaskSummary.success(file)
    reportProgress(resultSummary, file)
    resultSummary
  }

  private def deleteDir(dir: VDirectory, summary: IOTaskSummary): IOTaskSummary = {
//    logger.trace(s"Deleting dir: $dir")

    reportProgress(summary, dir)

    val dirSummary = dir.childDirs.foldLeft(summary)((result, childDir) => deleteDir(childDir, result))
    val totalSummary = dir.childFiles.foldLeft(dirSummary)((result, childFile) => deleteFile(childFile, result))

    if (totalSummary.errors.isEmpty) performDelete(dir)
    else logger.info(s"There was an error while deleting child files/directories. Skipping delete of $dir")

    val resultSummary = totalSummary + IOTaskSummary.success(dir)
    reportProgress(resultSummary, dir)
    resultSummary
  }

  private def performDelete(path: VPath): Unit = {
    if (!dryRun) {
      val fs = path.fileSystem
      fs.delete(path)
    }
  }

  private def reportProgress(summary: IOTaskSummary, curPath: VPath): IOProgress = {
    val progress = IOProgress(None, summary, Some(curPath.absolutePath), jobStats)
    updateValue(progress)
    progress
  }

  private def reportProgress(summary: IOTaskSummary): IOProgress = {
    val progress = IOProgress(None, summary, None, jobStats)
    updateValue(progress)
    progress
  }
}
