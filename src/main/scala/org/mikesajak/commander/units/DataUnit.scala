package org.mikesajak.commander.units

import enumeratum._

import scala.collection.immutable

sealed abstract class DataUnit(val multiplier: Long, val symbol: String) extends EnumEntry {
  def convert(value: Double): Double = value / multiplier.toDouble
  def format(value: Double, unitStep: String = " ") = f"${convert(value)}%.2f$unitStep$symbol"
}

object DataUnit extends Enum[DataUnit] {
  override val values: immutable.IndexedSeq[DataUnit] = findValues

  case object Byte extends DataUnit(1, "B")
  case object KiloByte extends DataUnit(1000, "kB")
  case object MegaByte extends DataUnit(1000 * KiloByte.multiplier, "MB")
  case object GigaByte extends DataUnit(1000 * MegaByte.multiplier, "GB")
  case object TerraByte extends DataUnit(1000 * GigaByte.multiplier, "TB")
  case object PetaByte extends DataUnit(1000 * TerraByte.multiplier, "PB")

  def formatDataSize(value: Double): String = findDataSizeUnit(value).format(value)

  def findDataSizeUnit(value: Double, threshold: Double = 10000): DataUnit = {
    DataUnit.values.find(u => u.convert(value) < threshold)
        .getOrElse(DataUnit.values.last)
  }

  def mkDataSize(value: Double, threshold: Double = 10000) =
    DataSize(value, findDataSizeUnit(value, threshold))
}

case class DataSize(value: Double, unit: DataUnit) {
  override def toString: String = unit.format(value)
}
