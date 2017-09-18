package org.mikesajak.commander.task

trait ProgressMonitor {
  def updateIndeterminate(message: String)
  def updateDeterminate(progress: Float, message: Option[String] = None)

  def notifyFinished(message: String)
  def notifyError(errMessage: String)
}

class ConsoleProgressMonitor extends ProgressMonitor {
  override def updateIndeterminate(message: String): Unit = println(message)

  override def updateDeterminate(progress: Float, message: Option[String] = None): Unit =
    println(s"$progress% - $message")

  override def notifyFinished(message: String): Unit = println(s"Finished task: $message")

  override def notifyError(errMessage: String): Unit = println(s"Error executing task: $errMessage")
}

trait ProgressMonitor2[A] {
  def notifyProgressIndeterminate(message: Option[String], state: Option[A])
  def notifyProgress(progress: Float, message: Option[String], state: Option[A])

  def notifyFinished(message: String, state: Option[A])
  def notifyError(message: String, state: Option[A])
}

class ConsoleProgressMonitor2[A] extends ProgressMonitor2[A] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[A]): Unit =
    println(s"$message, state=$state")

  override def notifyProgress(progress: Float, message: Option[String], state: Option[A]): Unit =
    println(s"$progress% - $message, state=$state")

  override def notifyFinished(message: String, state: Option[A]): Unit = println(s"Finished task: $message, state=$state")

  override def notifyError(message: String, state: Option[A]): Unit = println(s"Error executing task: $message, state=$state")
}
