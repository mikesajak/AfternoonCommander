package org.mikesajak.commander.task

trait Task[A] {
  def run(progressMonitor: ProgressMonitor2[A]): A
}
