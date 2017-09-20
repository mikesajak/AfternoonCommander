package org.mikesajak.commander.util

import scala.language.reflectiveCalls

/**
  * Created by mike on 07.05.17.
  */
object Utils {
  type ClosableResource = { def close() }

  def using[A <: ClosableResource, B](res: A)(f: A => B): B =
    try {
      f(res)
    } finally {
      res.close()
    }

  def using[B](closable: => Unit)(f: => B): B =
    try {
      f
    } finally {
      closable
    }
}
