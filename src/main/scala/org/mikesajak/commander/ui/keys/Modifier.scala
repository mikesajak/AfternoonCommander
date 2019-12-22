package org.mikesajak.commander.ui.keys

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait Modifier extends EnumEntry

object Modifier extends Enum[Modifier] {
  override val values: immutable.IndexedSeq[Modifier] = findValues

  case object Alt extends Modifier
  case object Ctrl extends Modifier
  case object Shift extends Modifier
  case object Meta extends  Modifier

  def complementOf(entries: Seq[Modifier]): Seq[Modifier] = {
    val modSet = entries.toSet
    values.filter(e => !modSet.contains(e))
  }
}