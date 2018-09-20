package org.mikesajak.commander.ui

import com.google.common.eventbus.Subscribe
import org.mikesajak.commander.EventBus
import org.mikesajak.commander.task.{IOTaskSummary, ProgressMonitor}

case class ProgressUpdate(totalProgress: Option[Float], partialProgress: Option[Float], message: Option[String], state: Option[IOTaskSummary])
case class ProgressUpdateFinished(message: Option[String], state: Option[IOTaskSummary])
case class ProgressUpdateError(message: String, state: Option[IOTaskSummary])
case class ProgressUpdateAbort(message: Option[String])

class EventBusAwareProgressMonitor(eventBus: EventBus) extends ProgressMonitor[IOTaskSummary] {

  override def notifyProgressIndeterminate(message: Option[String], state: Option[IOTaskSummary]): Unit =
    eventBus.publish(ProgressUpdate(None, None, message, state))

  override def notifyProgress(progress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
    eventBus.publish(ProgressUpdate(Some(progress), None, message, state))

  override def notifyDetailedProgress(partProgress: Float, totalProgress: Float, message: Option[String], state: Option[IOTaskSummary]): Unit =
    eventBus.publish(ProgressUpdate(Some(totalProgress), Some(partProgress), message, state))

  override def notifyFinished(message: Option[String], state: Option[IOTaskSummary]): Unit =
    eventBus.publish(ProgressUpdateFinished(message, state))

  override def notifyError(message: String, state: Option[IOTaskSummary]): Unit =
    eventBus.publish(ProgressUpdateError(message, state))

  override def notifyAborted(message: Option[String]): Unit =
    eventBus.publish(ProgressUpdateAbort(message))
}

trait EventBusProgressReceiver {
  @Subscribe
  def stepUpdate(progressUpdate: ProgressUpdate)

  @Subscribe
  def finished(progressUpdate: ProgressUpdateFinished)

  @Subscribe
  def errored(progressUpdate: ProgressUpdateError)

  @Subscribe
  def aborted(progressUpdate: ProgressUpdateAbort)
}