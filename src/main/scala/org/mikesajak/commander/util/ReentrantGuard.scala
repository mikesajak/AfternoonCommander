package org.mikesajak.commander.util

class ReentrantGuard {
  private var pending = false

  def guard[A](f: () => Unit): Unit = {
    if (!pending) {
      try {
        pending = true
        f()
      } finally {
        pending = false
      }
    }
  }
}
