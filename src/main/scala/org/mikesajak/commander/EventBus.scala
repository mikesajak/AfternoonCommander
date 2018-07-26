package org.mikesajak.commander

import com.google.common.eventbus.{DeadEvent, Subscribe}
import com.typesafe.scalalogging.Logger

class EventBus {
  private val eventBus = new com.google.common.eventbus.EventBus("App event bus")
  eventBus.register(new DeadEventHandler)

  def publish[A](event: A): Unit = eventBus.post(event)
  def register(subscriber: AnyRef): Unit = eventBus.register(subscriber)
  def unregister(subscriber: AnyRef): Unit = eventBus.unregister(subscriber)

}

class DeadEventHandler {
  private val logger = Logger[DeadEventHandler]
  @Subscribe
  def handleDeadEvent(de: DeadEvent): Unit = {
    logger.debug(s"$de")
  }
}
