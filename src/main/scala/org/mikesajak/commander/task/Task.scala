package org.mikesajak.commander.task

trait Task[A] {
  def run(progressMonitor: ProgressMonitor[A]): A
}
