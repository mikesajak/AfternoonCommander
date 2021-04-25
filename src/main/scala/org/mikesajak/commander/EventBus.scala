package org.mikesajak.commander

import com.google.common.eventbus.{DeadEvent, Subscribe}
import scribe.Logging

//noinspection UnstableApiUsage
class EventBus extends Logging {
  private val eventBus = new com.google.common.eventbus.EventBus("App event bus")
  eventBus.register(new DeadEventHandler)

  def logScope[A](name: => String)(code: () => A): A = {
    logger.trace(s"Start: $name")
    val r = code()
    logger.trace(s"End: $name")
    r
  }

  def publish[A](event: A): Unit = {
    logScope(s"Publishing event: $event") { () =>
      eventBus.post(event)
    }
  }

  def register(subscriber: AnyRef): Unit = eventBus.register(subscriber)
  def unregister(subscriber: AnyRef): Unit = eventBus.unregister(subscriber)

}

//noinspection UnstableApiUsage
class DeadEventHandler extends Logging {
  @Subscribe
  def handleDeadEvent(de: DeadEvent): Unit = {
    logger.debug(s"$de")
  }
}
