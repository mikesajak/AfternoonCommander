package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger

trait Cancellable {
  val cancelSupported = true
  def cancel(): Unit
}

trait CancellableTask extends Cancellable {
  @volatile
  private var abort: Boolean = false

  override def cancel(): Unit = abort = true

  protected def abortIfNeeded(): Unit = if (abort) throw new AbortOperationException
}

object CancellableTask {
  private val logger = Logger[CancellableTask]

  implicit def withAbort[A](progressMonitor: ProgressMonitor[A])(code: () => A): Option[A] = {
    try {
      Some(code())
    } catch {
      case e: AbortOperationException =>
        val msg = Option(e.getLocalizedMessage)
        logger.debug(s"Task aborted, message=$msg")
        progressMonitor.notifyAborted(msg)
        None
    }
  }
}
