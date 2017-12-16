package org.mikesajak.commander.task

trait ProgressMonitor[A] {
  def notifyProgressIndeterminate(message: Option[String], state: Option[A])
  def notifyProgress(progress: Float, message: Option[String], state: Option[A])
  def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[A])

  def notifyFinished(message: Option[String], state: Option[A])
  def notifyError(message: String, state: Option[A])

  def notifyAborted(message: String)
}

object ProgressMonitor {
  def runWithProgress[B](name: String)(f: () => B)(implicit progressMonitor: ProgressMonitor[B]): B = {
    progressMonitor.notifyProgressIndeterminate(Some(s"$name started"), None)
    val result = f()
    progressMonitor.notifyProgressIndeterminate(Some(s"$name finished"), Some(result))
    result
  }

  def runWithProgress[A, B](name: String, a: A)(f: A => B)(implicit progressMonitor: ProgressMonitor[B]): B = {
    progressMonitor.notifyProgressIndeterminate(Some(s"$name started"), None)
    val result = f(a)
    progressMonitor.notifyProgressIndeterminate(Some(s"$name finished"), Some(result))
    result
  }
}

class ConsoleProgressMonitor[A] extends ProgressMonitor[A] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[A]): Unit =
    println(s"$message, state=$state")

  override def notifyProgress(progress: Float, message: Option[String], state: Option[A]): Unit =
    println(s"$progress% - $message, state=$state")

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[A]): Unit =
    println(s"$partProgress% / $totalProgress% - $message, state=$state")

  override def notifyFinished(message: Option[String], state: Option[A]): Unit = println(s"Finished task: $message, state=$state")

  override def notifyError(message: String, state: Option[A]): Unit = println(s"Error executing task: $message, state=$state")

  override def notifyAborted(message: String): Unit = println(s"Task aborted: $message")
}

class MultiProgressMonitor[A](childMonitors: Seq[ProgressMonitor[A]]) extends ProgressMonitor[A] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[A]): Unit =
    childMonitors.foreach(_.notifyProgressIndeterminate(message, state))

  override def notifyProgress(progress: Float, message: Option[String], state: Option[A]): Unit =
    childMonitors.foreach(_.notifyProgress(progress, message, state))

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[A]): Unit =
    childMonitors.foreach(_.notifyDetailedProgress(partProgress, totalProgress, message, state))

  override def notifyFinished(message: Option[String], state: Option[A]): Unit =
    childMonitors.foreach(_.notifyFinished(message, state))

  override def notifyError(message: String, state: Option[A]): Unit =
    childMonitors.foreach(_.notifyError(message, state))

  override def notifyAborted(message: String): Unit =
    childMonitors.foreach(_.notifyAborted(message))
}

object MultiProgressMonitor {
  def apply[A](pms: ProgressMonitor[A]*) = new MultiProgressMonitor(pms)
}
