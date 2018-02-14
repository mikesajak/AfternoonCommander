package org.mikesajak.commander.task

trait Cancellable {
  val cancelSupported = true
  def cancel(): Unit
}

trait CancellableTask//[A]
  extends //Task[A] with
  Cancellable {
  @volatile
  private var abort: Boolean = false

//  override val cancelSupported = true
  override def cancel(): Unit = abort = true

  protected def abortIfNeeded(): Unit = if (abort) throw new AbortOperationException
}

object CancellableTask {
  implicit def withAbort[A](progressMonitor: ProgressMonitor[A])(code: () => A): Option[A] = {
    try {
      Some(code())
    } catch {
      case e: AbortOperationException =>
        progressMonitor.notifyAborted(e.getLocalizedMessage)
        None
    }
  }
}
