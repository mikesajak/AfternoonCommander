package org.mikesajak.commander.task

import javafx.concurrent.Worker.State
import scribe.Logging

class BackgroundServiceRegistry extends Logging {

  private var serviceList = List[(BackgroundService[_], String, Boolean)]()

  def registerServiceFor[A](task: => scalafx.concurrent.Task[A], autoremove: Boolean = true): BackgroundService[A] = {
    val svc = registerTask(task, autoremove)

    svc.state.onChange { (_, _, state) =>

      state match {
        case State.SCHEDULED =>
        case State.READY =>
        case State.RUNNING =>

        case State.FAILED | State.CANCELLED | State.SUCCEEDED =>
          if (autoremove) unregisterService(svc)
      }
    }
    svc
  }

  private def registerTask[A](task: scalafx.concurrent.Task[A], autoremove: Boolean = true)= {
    val service = new BackgroundService(task)
    serviceList ::= (service, task.title.value, autoremove)
    logger.debug(s"Registered service: ${task.title.value}.")
    logCurListServices()
    service
  }

  private def unregisterService[A](service: BackgroundService[A]): Unit = {
    if (serviceList.exists(entry => entry._1 == service)) {
      serviceList = serviceList.filterNot(s => s._1 == service)
      logger.debug(s"Unregistered service: ${service.title.value}")
    } else logger.debug(s"Cannot unregister service ${service.title.value}. It's not found on registration list.")

    logCurListServices()
  }

  private def logCurListServices(): Unit = {
    logger.debug(s"Current list of services: ${serviceList.map(s => s"(${s._2}, ${s._3})")}")
  }

  def services: Seq[BackgroundService[_]] = serviceList.map(_._1)

}
