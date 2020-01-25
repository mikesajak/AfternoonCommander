package org.mikesajak.commander.ui

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

object Action extends Enum[Action] {
  override val values: immutable.IndexedSeq[Action] = findValues

  case object View extends Action
  case object Edit extends Action
  case object Copy extends Action
  case object Move extends Action
  case object MkDir extends Action
  case object Delete extends Action
  case object Exit extends Action

  case object FindFiles extends Action
  case object CountDirStats extends Action
  case object ShowProperties extends Action

  case object Refresh extends Action
  case object SwitchSelectedPanel extends Action
}

sealed trait Action extends EnumEntry