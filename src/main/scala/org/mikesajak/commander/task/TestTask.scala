package org.mikesajak.commander.task

object TestTask {
  private var taskId = 0

  def taskCreated() = synchronized {
    val id = taskId
    taskId += 1
    id
  }
}

class TestTask extends Task[Unit] {
  private val id = TestTask.taskCreated()

  override def run(progressMonitor: ProgressMonitor[Unit]) = {
    for (i <- 0 to 100) {
      progressMonitor.notifyProgress(i, Some(s"TestTask($id)"), None)
      Thread.sleep(50)
    }
    progressMonitor.notifyFinished(Some(s"TestTask($id)"), None)

    Some(Unit)
  }

  override val cancelSupported: Boolean = false

  override def cancel(): Unit = throw new UnsupportedOperationException("Cancel is not supported")
}
