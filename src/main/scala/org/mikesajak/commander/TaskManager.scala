package org.mikesajak.commander

import org.mikesajak.commander.task.{ProgressMonitor, Task}

import scala.concurrent.Future

class TaskManager {
  def runTaskSync(task: Task, progressMonitor: ProgressMonitor): Unit = {
    println(s"Running task: $task")
    task.run(progressMonitor)
    println(s"Finished task: $task")
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def runTaskAsync(task: Task, progressMonitor: ProgressMonitor): Future[Unit] = {
    Future[Unit] {
      task.run(progressMonitor)
    }

//    Observable.create(SyncOnSubscribe.stateful(() => 0)(i => {
//
//    }))
  }
}
