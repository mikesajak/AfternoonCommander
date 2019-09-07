package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.Utils.runWithTimer

import scala.util.{Failure, Success, Try}

case class DeleteJobDef(target: VPath)

class RecursiveDeleteTask(jobDefs: Seq[DeleteJobDef], jobStats: Option[DirStats], dryRun: Boolean,
                          resourceMgr: ResourceManager)
    extends jfxc.Task[IOProgress] {
  private implicit val logger: Logger = Logger[RecursiveDeleteTask]

  override def call(): IOProgress = {
    runWithTimer(s"Delete files task: $jobDefs")(runDelete)
  }

  private def runDelete(): IOProgress = try {
    val result = jobDefs.foldLeft(IOTaskSummary.empty) { case (summary, job) => delete(job.target, summary) }

    logger.debug(s"Finished delete task, result=$result")

    reportProgress(result)
  } catch {
    case _: CancelledException[_] =>
      logger.info(s"Task $this has been cancelled.")
      updateMessage(resourceMgr.getMessage("task.cancelled"))
      null
    case e: Exception =>
      updateMessage(e.getLocalizedMessage)
      throw e
  }

  private def delete(target: VPath, summary: IOTaskSummary) = {
    if (target.isDirectory) deleteDir(target.directory, summary)
    else deleteFile(target.asInstanceOf[VFile], summary)
  }

  private def deleteFile(file: VFile, summary: IOTaskSummary): IOTaskSummary = {
    checkCancelled()

    logger.trace(s"Deleting file: $file")

    reportProgress(summary, file)
    val resultSummary = summary + performDelete(file)
    reportProgress(resultSummary, file)
    resultSummary
  }

  private def deleteDir(dir: VDirectory, summary: IOTaskSummary): IOTaskSummary = {
    checkCancelled()

    logger.trace(s"Deleting dir: $dir")

    reportProgress(summary, dir)

    val dirSummary = dir.childDirs.foldLeft(summary)((result, childDir) => deleteDir(childDir, result))
    val totalSummary = dir.childFiles.foldLeft(dirSummary)((result, childFile) => deleteFile(childFile, result))

    val result =
      if (totalSummary.errors.isEmpty) performDelete(dir)
      else {
        val msg = resourceMgr.getMessageWithArgs("delete_task.skipping_dir", List(dir))
        logger.info(msg)
        IOTaskSummary.failed(dir, msg)
      }

    val resultSummary = totalSummary + result
    reportProgress(resultSummary, dir)
    resultSummary
  }

  private def checkCancelled(): Unit = {
    if (isCancelled) {
      logger.debug(s"Cancel request was detected - stopping current task.")
      throw new CancelledException
    }
  }

  private def performDelete(path: VPath): IOTaskSummary = {
    doDelete(path) match {
      case Success(true) => IOTaskSummary.success(path)
      case Success(false) if dryRun => IOTaskSummary.success(path)
      case Success(false) if !dryRun => IOTaskSummary.failed(path, resourceMgr.getMessageWithArgs("delete_task.couldnt_delete", List(path)))
      case Failure(exception) => IOTaskSummary.failed(path, exception.getLocalizedMessage)
    }
  }

  private def doDelete(path: VPath): Try[Boolean] = {
    if (!dryRun) {
      val fs = path.fileSystem
      fs.delete(path)
    } else Success(true)
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
