package org.mikesajak.commander

import org.mikesajak.commander.task.{ProgressMonitor2, Task}

import scala.concurrent.Future

class TaskManager {
  def runTaskSync[A, B](task: Task[A, B], progressMonitor: ProgressMonitor2[B]): Unit = {
    println(s"Running task: $task")
    task.run(progressMonitor)
    println(s"Finished task: $task")
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def runTaskAsync[A, B](task: Task[A, B], progressMonitor: ProgressMonitor2[B]): Future[A] = {
    Future[A] {
      task.run(progressMonitor)
    }
  }
}
