package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import scalafx.Includes._
import scalafx.concurrent.{Service, WorkerStateEvent}

class BackgroundService[A](taskProducer: () => scalafx.concurrent.Task[A])(implicit manifest: Manifest[A])
    extends Service[A](new jfxc.Service[A]() {
    override def createTask(): jfxc.Task[A] = taskProducer()
  }) {
  private val logger = Logger[BackgroundService[A]]

  this.handleEvent(WorkerStateEvent.ANY) { e: WorkerStateEvent =>
    logger.debug(s"${e.eventType} ${title.value}")
  }
}
