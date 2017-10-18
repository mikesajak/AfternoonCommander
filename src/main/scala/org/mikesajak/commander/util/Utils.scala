package org.mikesajak.commander.util

import java.io.{PrintWriter, StringWriter}

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

  def getStackTraceText(e: Exception): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }

  implicit class MyRichBoolean(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Some(a) else None
  }
}
