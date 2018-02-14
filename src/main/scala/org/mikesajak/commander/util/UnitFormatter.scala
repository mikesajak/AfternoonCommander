package org.mikesajak.commander.util

object UnitFormatter {
  val Units = IndexedSeq("", "k", "M", "G", "T", "P")

  object DataSizeUnit extends Enumeration {
    sealed abstract class DataSizeUnit(multiplier: Long, symbol: String) {
      def convert(value: Double): Double = value / multiplier.toDouble
      def format(value: Double) = s"${convert(value)}$symbol"
    }
    case object Byte extends DataSizeUnit(1, "B")
    case object KiloByte extends DataSizeUnit(1000, "kB")
    case object MegaByte extends DataSizeUnit(1000000, "MB")
    case object GigaByte extends DataSizeUnit(1000000000, "GB")
    case object TerraByte extends DataSizeUnit(1000000000000L, "TB")

    val Units = IndexedSeq(Byte, KiloByte, MegaByte, GigaByte, TerraByte)
  }

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
    s"$strValue $unit"
  }

  def formatNumElements(numElems: Int): String = {
    val unit = if (numElems == 1) "element" else "elements"
    s"$numElems $unit"
  }

  def findNearestUnit(value: Double, threshold: Double): DataSizeUnit.DataSizeUnit = {
    val it = DataSizeUnit.Units.iterator
    DataSizeUnit.Units.find(u => u.convert(value) < threshold)
                      .getOrElse(DataSizeUnit.Units.last)
  }

}
