package org.mikesajak.commander.util

import java.util.{Timer, TimerTask}

class Throttler[A](minUpdateTime: Long, updateFunc: A => Unit) {
  @volatile
  private var state: A = _
  @volatile
  private var lastUpdated: Long = 0

  private val timer = new Timer
  private var task: TimerTask = _

  def update(newState: A): Unit = {
    state = newState
    val curTime = System.currentTimeMillis()

    if (task != null) {
      task.cancel()
      task = null
    }

    if (curTime - lastUpdated >= minUpdateTime) {
      updateFunc(state)
      lastUpdated = curTime
    } else {
      task = new TimerTask() { def run() {updateFunc(state)} }
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

}
