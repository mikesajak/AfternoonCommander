package org.mikesajak.commander.task

trait ProgressMonitor {
  def updateIndeterminate(message: String)
  def updateDeterminate(progress: Float, message: Option[String])

  def notifyFinished(message: String)
  def notifyError(errMessage: String)
}

class ConsoleProgressMonitor extends ProgressMonitor {
  override def updateIndeterminate(message: String): Unit = println(message)

  override def updateDeterminate(progress: Float, message: Option[String]): Unit = println(s"$progress% - $message")

  override def notifyFinished(message: String): Unit = println(s"Finished task: $message")

  override def notifyError(errMessage: String): Unit = println(s"Error executing task: $errMessage")
}