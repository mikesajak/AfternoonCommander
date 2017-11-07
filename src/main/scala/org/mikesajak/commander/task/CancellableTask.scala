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

  protected def abortIfNeeded(): Unit = if (abort) throw new AbortTaskException
}

object CancellableTask {
  implicit def withAbort[A](code: () => A)(implicit progressMonitor: ProgressMonitor[A]): Option[A] = {
    try {
      Some(code())
    } catch {
      case e: AbortTaskException =>
        progressMonitor.notifyAborted(e.getLocalizedMessage)
        None
    }
  }
}
