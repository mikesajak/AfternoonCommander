package org.mikesajak.commander.util

object UnitFormatter {
  object DataSizeUnit extends Enumeration {
    sealed abstract class DataSizeUnit(multiplier: Long, symbol: String) {
      def convert(value: Double): Double = value / multiplier.toDouble
      def format(value: Double, unitSep: String = " ") = //s"${convert(value)}$symbol"
          f"${convert(value)}%.2f$unitSep$symbol"
    }
    case object Byte extends DataSizeUnit(1, "B")
    case object KiloByte extends DataSizeUnit(1000, "kB")
    case object MegaByte extends DataSizeUnit(1000000, "MB")
    case object GigaByte extends DataSizeUnit(1000000000, "GB")
    case object TerraByte extends DataSizeUnit(1000000000000L, "TB")

    val Units = IndexedSeq(Byte, KiloByte, MegaByte, GigaByte, TerraByte)
  }

  def formatDataSize(value: Double): String = findDataSizeUnit(value).format(value)

  def findDataSizeUnit(value: Double, threshold: Double = 10000): DataSizeUnit.DataSizeUnit = {
    val it = DataSizeUnit.Units.iterator
    DataSizeUnit.Units.find(u => u.convert(value) < threshold)
                      .getOrElse(DataSizeUnit.Units.last)
  }

  def formatNumElements(numElems: Int): String = {
    val unit = if (numElems == 1) "element" else "elements"
    s"$numElems $unit"
  }
}
