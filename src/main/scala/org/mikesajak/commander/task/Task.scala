package org.mikesajak.commander.task

trait Task[A] {
  def run(progressMonitor: ProgressMonitor[A]): A
  val cancelSupported: Boolean = false
  def cancel(): Unit = throw new UnsupportedOperationException(s"Cancel is not supported for this task: ${this.getClass.getSimpleName}")
}
