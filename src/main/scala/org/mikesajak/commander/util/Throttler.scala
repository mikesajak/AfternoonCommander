package org.mikesajak.commander.util

import javafx.concurrent.Worker.State
import scalafx.concurrent.Service
import scalafx.event.subscriptions.Subscription

import java.util.{Timer, TimerTask}

object Throttler {
  val commonTimer = new Timer()

  def registerCancelOnServiceFinish(service: Service[_], throttler: Throttler[_]): Subscription = {
    service.state.onChange { (_, _, state) => state match {
        case State.CANCELLED | State.FAILED | State.SUCCEEDED => throttler.cancel()
        case _ =>
      }
    }
  }
}

class Throttler[A](minUpdateTime: Long, updateFunc: A => Unit, timer0: => Timer = Throttler.commonTimer) {
  @volatile
  private var state: A = _
  @volatile
  private var lastUpdated: Long = 0

  private val timer = timer0
  private var task: TimerTask = _

  def update(newState: A): Unit = {
    state = newState
    val curTime = System.nanoTime()

    cancel()

    if (nanoToMilli(curTime - lastUpdated) >= minUpdateTime) {
      updateFunc(state)
      lastUpdated = curTime
    } else {
      task = new TimerTask() { def run(): Unit = {updateFunc(state)} }
      timer.schedule(task, minUpdateTime)
    }
  }

  def cancel(): Unit = {
    if (task != null) {
      task.cancel()
      task = null
    }
  }

  def getState: A = state

  @inline
  private def nanoToMilli(nano: Long) = nano / 1000000L

}
