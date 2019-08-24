package org.mikesajak.commander.util

object Implicits {
  implicit class RichOption[A](option: Option[A]) {
    def runIfEmpty(action: => Unit): Option[A] = {
      if (option.isEmpty)
        action
      option
    }
  }
}
