package org.mikesajak.commander

import org.mikesajak.commander.task.{ProgressMonitor2, Task}

import scala.concurrent.Future

class TaskManager {
  def runTaskSync[A](task: Task[A], progressMonitor: ProgressMonitor2[A]): Unit = {
    println(s"Running task: $task")
    task.run(progressMonitor)
    println(s"Finished task: $task")
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def runTaskAsync[A](task: Task[A], progressMonitor: ProgressMonitor2[A]): Future[A] = {
    Future[A] {
      task.run(progressMonitor)
    }
  }
}
