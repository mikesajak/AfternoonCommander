package org.mikesajak.commander.util

import java.util.{Timer, TimerTask}

object Throttler {
  val commonTimer = new Timer()
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
      task = new TimerTask() { def run() { updateFunc(state)} }
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
