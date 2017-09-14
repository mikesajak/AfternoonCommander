package org.mikesajak.commander

import org.mikesajak.commander.task.{ProgressMonitor, Task}

class TaskManager {
  def runTaskConcurrently(task: Task, progressMonitor: ProgressMonitor): Unit = {
    println(s"Running task: $task")
    task.run(progressMonitor)
    println(s"Finished task: $task")
  }
}
