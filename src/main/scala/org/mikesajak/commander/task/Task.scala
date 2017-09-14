package org.mikesajak.commander.task

trait Task {
  def run(progressMonitor: ProgressMonitor)
}
