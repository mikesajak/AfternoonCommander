package org.mikesajak.commander.task

import org.mikesajak.commander.util.DataUnit

// TODO: Remove this class
trait ProgressMonitor[A] {
  def notifyProgressIndeterminate(message: Option[String], state: Option[A])
  def notifyProgress(progress: Float, message: Option[String], state: Option[A])
  def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[A])

  def notifyFinished(message: Option[String], state: Option[A])
  def notifyError(message: String, state: Option[A])

  def notifyAborted(message: Option[String])
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

class IOConsoleProgressMonitor extends ProgressMonitor[IOTaskSummary] {override def notifyProgressIndeterminate(message: Option[String], state: Option[IOTaskSummary]): Unit = ???
  private val consoleProgressMonitor = new ConsoleProgressMonitor[(String, IOTaskSummary)]

  case class Report(timestamp: Long, summary: IOTaskSummary)
  object Report {
    def current(summary: IOTaskSummary) = new Report(System.currentTimeMillis, summary)

    def diff(report1: Report, report2: Report): String = {
      val timeDiffInSeconds = (report2.timestamp - report1.timestamp) / 1000.0
      val sizeDiff = report2.summary.totalSize - report1.summary.totalSize

      val unit = DataUnit.findDataSizeUnit(sizeDiff, 10000)
      s"${unit.format(sizeDiff)}/${timeDiffInSeconds}s"
    }
  }

  private var previousReport: Report = null

  private def handleStateChange(state: Option[IOTaskSummary]) = {
    if (previousReport == null) {
      if (state.isDefined)
        previousReport = Report.current(state.get)

      ("", state.orNull)
    } else {
      if (state.isDefined) {
        val curReport = Report.current(state.get)
        val diffReport = Report.diff(previousReport, curReport)
        previousReport = curReport
        (diffReport, state.get)
      } else {
        ("", null)
      }
    }
  }

  override def notifyProgress(progress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit = {

  }

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit = ???

  override def notifyFinished(message: Option[String], state: Option[IOTaskSummary]): Unit = ???

  override def notifyError(message: String, state: Option[IOTaskSummary]): Unit = ???

  override def notifyAborted(message: Option[String]): Unit = ???
}

class ConsoleProgressMonitor[A] extends ProgressMonitor[A] {
  override def notifyProgressIndeterminate(message: Option[String], state: Option[A]): Unit =
    println(s"Notify indeterminate progress: message=$message, state=$state")

  override def notifyProgress(progress: Float, message: Option[String], state: Option[A]): Unit =
    println(s"Notify progress: $progress% - message=$message, state=$state")

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[A]): Unit =
    println(s"Notify detailed progress: $partProgress% / $totalProgress% - message=$message, state=$state")

  override def notifyFinished(message: Option[String], state: Option[A]): Unit = println(s"Finished task: $message, state=$state")

  override def notifyError(message: String, state: Option[A]): Unit = println(s"Error executing task: $message, state=$state")

  override def notifyAborted(message: Option[String]): Unit = println(s"Task aborted: $message")
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

  override def notifyAborted(message: Option[String]): Unit =
    childMonitors.foreach(_.notifyAborted(message))
}

object MultiProgressMonitor {
  def apply[A](pms: ProgressMonitor[A]*) = new MultiProgressMonitor(pms)
}
