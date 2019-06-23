package org.mikesajak.commander.util

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class TimeUnit(val multiplier: Long, val symbol: String) extends EnumEntry {
  def calculate(millis: Long): (Long, Long) = {
    val numFull = millis / multiplier
    (numFull, millis - numFull * multiplier)
  }

  override def toString: String = symbol
}

object TimeUnit extends Enum[TimeUnit] {
  override val values: immutable.IndexedSeq[TimeUnit] = findValues

  case object Millisecond extends TimeUnit(1, "ms")
  case object Second extends TimeUnit(1000 * Millisecond.multiplier, "s")
  case object Minute extends TimeUnit(60 * Second.multiplier, "m")
  case object Hour extends TimeUnit(60 * Minute.multiplier, "h")
  case object Day extends TimeUnit(24 * Hour.multiplier, "d")
}

object TimeInterval {
  def apply(totalMillis: Long): TimeInterval = {
    val units = TimeUnit.values.reverse

    val counts = units.scanLeft((0L, totalMillis)) { (curMillis, unit) => unit.calculate(curMillis._2) }
         .drop(1)
         .map(_._1).zip(units)

    TimeInterval(counts)
  }
}

case class TimeInterval(units: Seq[(Long, TimeUnit)]) {
  def format(sep: String = " "): String =
    units.dropWhile(_._1 == 0)
      .foldLeft("")((str, u) => s"$str$sep${u._1}${u._2}")

  override def toString: String = format()
}

