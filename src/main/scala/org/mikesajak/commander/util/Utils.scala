package org.mikesajak.commander.util

import java.io.{PrintWriter, StringWriter}

import com.google.common.base.Stopwatch
import com.typesafe.scalalogging.Logger

import scala.language.{implicitConversions, reflectiveCalls}

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

  object LogLevel extends Enumeration {
    type LogLevel = Value
    val Trace, Debug, Info, Warn, Error = Value
  }

  import LogLevel._

  def log(msg: => String, level: LogLevel)(implicit logger: Logger): Unit = level match {
    case Trace => logger.trace(msg)
    case Debug => logger.debug(msg)
    case Info => logger.info(msg)
    case Warn => logger.warn(msg)
    case Error => logger.error(msg)
  }

  def log(msg: => String, ex: Exception, level: LogLevel)(implicit logger: Logger): Unit = level match {
    case Trace => logger.trace(msg, ex)
    case Debug => logger.debug(msg, ex)
    case Info => logger.info(msg, ex)
    case Warn => logger.warn(msg, ex)
    case Error => logger.error(msg, ex)
  }

  def runWithTimer[B](name: String, stepLogLovel: LogLevel = Trace,
                      successLogLevel: LogLevel = Debug, errorLogLevel: LogLevel = Error)
                     (code: () => B)(implicit logger: Logger): B = {
    val stopwatch = Stopwatch.createStarted()
    try {
      log(s"$name started", stepLogLovel)
      val result = code()
      log(s"$name finished in $stopwatch", successLogLevel)
      result
    } catch {
      case e: Exception =>
        log(s"$name finisded in $stopwatch with error $e", e, errorLogLevel)
        throw e
    }
  }

  def getStackTraceText(e: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }

  implicit class MyRichBoolean(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Some(a) else None
  }

  implicit def toRunnableConversion(f: () => Unit) = new Runnable() {
    override def run(): Unit = f()
  }
}
