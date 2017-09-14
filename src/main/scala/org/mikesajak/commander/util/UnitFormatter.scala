package org.mikesajak.commander.util

object UnitFormatter {

  private def unitFor(num: Double, step: Double, level: Int = 0): (Double, Int) = {
    if (num > step) {
      val sub = num / step
      unitFor(sub, step, level + 1)
    } else (num, level)
  }

  val byteUnits = IndexedSeq("", "k", "M", "G", "T", "P")

  def byteUnit(num: Double) = {
    val (value, unit) = unitFor(num, 1024)
    (value, s"${byteUnits(unit)}B")
  }

}
