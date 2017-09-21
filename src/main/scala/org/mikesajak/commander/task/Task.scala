package org.mikesajak.commander.task

trait Task[A, B] {
  def run(progressMonitor: ProgressMonitor2[B]): A
}
