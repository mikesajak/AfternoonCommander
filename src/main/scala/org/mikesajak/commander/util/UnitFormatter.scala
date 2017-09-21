package org.mikesajak.commander.util

object UnitFormatter {
  val Units = IndexedSeq("", "k", "M", "G", "T", "P")

  private def unitFor(num: Double, threshold: Double, step: Double, level: Int = 0): (Double, Int) = {
    if (num > threshold) {
      val sub = num / step
      unitFor(sub, threshold, step, level + 1)
    } else (num, level)
  }

  def byteUnit(num: Double, raw: Boolean = false): (Double, String) = {
    val (value, unit) = unitFor(num, 100000, 1000)
    val byteSymbol = if (raw) "" else "B"
    (value, s"${Units(unit)}$byteSymbol")
  }

  def formatUnit(num: Double, raw: Boolean = false): String = {
    val (value, unit) = byteUnit(num, raw)
    val strValue = if (value == value.toLong) value.toLong.toString else f"$value%.2f"
    s"$strValue$unit"
  }

}
