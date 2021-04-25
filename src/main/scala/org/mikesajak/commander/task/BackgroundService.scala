package org.mikesajak.commander.task

import javafx.{concurrent => jfxc}
import scalafx.Includes._
import scalafx.concurrent.{Service, WorkerStateEvent}

class BackgroundService[A](task: => scalafx.concurrent.Task[A])
    extends Service[A](new jfxc.Service[A]() {
    override def createTask(): jfxc.Task[A] = task
  }) {

  this.handleEvent(WorkerStateEvent.ANY) { e: WorkerStateEvent =>
    scribe.debug(s"${e.eventType} ${title.value}")
  }
}
