package org.mikesajak.commander.task

object TestTask {
  private var taskId = 0

  def taskCreated() = synchronized {
    val id = taskId
    taskId += 1
    id
  }
}

class TestTask extends Task[Unit, Unit] {
  private val id = TestTask.taskCreated()

  override def run(progressMonitor: ProgressMonitor2[Unit]): Unit = {
    for (i <- 0 to 100) {
      progressMonitor.notifyProgress(i, Some(s"TestTask($id)"), None)
      Thread.sleep(50)
    }
    progressMonitor.notifyFinished(s"TestTask($id)", None)
  }
}
