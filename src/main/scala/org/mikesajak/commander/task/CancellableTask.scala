package org.mikesajak.commander.task

trait CancellableTask[A] extends Task[A]{
  @volatile
  private var abort: Boolean = false

  override val cancelSupported = true
  override def cancel(): Unit = abort = true

  protected def abortIfNeeded(): Unit = if (abort) throw new AbortTaskException
}
