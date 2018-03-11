package org.mikesajak.commander

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.task.{ProgressMonitor, Task}

import scala.concurrent.Future

class TaskManager {
  private val logger = Logger[TaskManager]

  def runTaskSync[A](task: Task[A], progressMonitor: ProgressMonitor[A]): Unit = {
    logger.info(s"Running task: $task")
    task.run(progressMonitor)
    logger.info(s"Finished task: $task")
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def runTaskAsync[A](task: Task[A], progressMonitor: ProgressMonitor[A]): Future[Option[A]] =
    Future {
      try {
        logger.info(s"Running async task: $task")
        val result = task.run(progressMonitor)
        logger.info(s"Finished async task: $task")
        result
      } catch {
        case e: Exception =>
          logger.info(s"Async task $task finished with exception: $e, ${e.getLocalizedMessage}")
          throw e
      }
    }
}
