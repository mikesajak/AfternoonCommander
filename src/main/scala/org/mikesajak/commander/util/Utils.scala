package org.mikesajak.commander.util

import com.google.common.base.Stopwatch
import org.mikesajak.commander.task.CancelledException
import scribe.Logger

import java.io.{PrintWriter, StringWriter}
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

  def using[A <: ClosableResource, B <: ClosableResource, R](res1: A, res2: B)(f: (A, B) => R): R =
    try {
      f(res1, res2)
    } finally {
      res1.close()
      res2.close()
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
                      finishLogLevel: LogLevel = Debug, errorLogLevel: LogLevel = Error)
                     (code: () => B)(implicit logger: Logger): B = {
    val stopwatch = Stopwatch.createStarted()
    try {
      log(s"$name started", stepLogLovel)
      val result = code()
      log(s"$name finished in $stopwatch", finishLogLevel)
      result
    } catch {
      case e: CancelledException[_] =>
        log(s"$name cancelled after $stopwatch", finishLogLevel)
        throw e
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

  def merge[K, V](maps: Map[K, V]*)(f: (K, V, V) => V): Map[K, V] = {
    maps.foldLeft(Map.empty[K, V]) { case (merged, m) =>
      m.foldLeft(merged) { case (acc, (k, v)) =>
        acc.get(k) match {
          case Some(existing) => acc.updated(k, f(k, existing, v))
          case None => acc.updated(k, v)
        }
      }
    }
  }

  trait Scope {
    def up(): Scope
    def down(): Scope
    def level: Int

    override def toString = s"Scope(level=$level)"
  }

  class IndentScope extends Scope {
    private var level0 = 0

    def up(): Scope = {
      level0 += 1
      this
    }
    def down(): Scope = {
      level0 -= 1
      this
    }

    def level: Int = level0

    def indent(step: Int = 2): String = " " * (step * level)

    override def toString: String = indent()
  }

  def scope[A, B](name: String, curScope: Scope, arg: A)(f: (Scope, A) => B)(implicit logger: Logger): B = {
    val scope = if (curScope == null) new IndentScope() else curScope.up()
    logger.debug(s"${scope}Entering $name")
    try {
      val res = f(scope, arg)
      scope.down()
      logger.debug(s"${scope}Exitting $name")
      res
    } catch {
      case e: Exception =>
        scope.down()
        logger.debug(s"${scope}Exitting with exception $name")
        throw e
    }
  }

  def logScope[A](name: String)(func: () => A)(implicit logger: Logger): A = try {
    logger.debug(s"Entering $name")
    val res = func()
    logger.debug(s"Exitting $name")
    res
  } catch {
    case e: Exception =>
      logger.debug(s"Exitting with exception $name")
      throw e
  }
}
