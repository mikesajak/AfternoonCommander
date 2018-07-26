package org.mikesajak.commander.util

import com.typesafe.scalalogging.Logger

class ReentrantGuard {
  private var pending = false
  private val logger = Logger[ReentrantGuard]

  def guard[A](f: () => Unit): Unit = {
    if (!pending) {
      try {
        logger.debug(s"Entering reentrant guard - ${System.identityHashCode(this)}")
        pending = true
        f()
      } finally {
        logger.debug(s"Exiting reentrant guard - ${System.identityHashCode(this)}")
        pending = false
      }
    } else {
      logger.trace(s"Skipping reentrant guard (pending) - ${System.identityHashCode(this)}")
    }
  }
}
