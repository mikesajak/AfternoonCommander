package org.mikesajak.commander.util

import enumeratum.{Enum, EnumEntry}
import scalafx.scene.input.KeyEvent

import scala.collection.immutable.IndexedSeq

object Keys {
  sealed trait Modifier extends EnumEntry
  object Modifier extends Enum[Modifier] {
    override val values: IndexedSeq[Modifier] = findValues

    case object Alt extends Modifier
    case object Ctrl extends Modifier
    case object Shift extends Modifier
    case object Meta extends  Modifier
    case object Shortcut extends Modifier

    def complementOf(entries: Seq[Modifier]): Seq[Modifier] = {
      val modSet = entries.toSet
      values.filter(e => !modSet.contains(e))
    }
  }

  import Modifier._

  def hasNoModifiers(ke: KeyEvent): Boolean = hasOnlyModifiers(ke)

  def hasOnlyModifiers(ke: KeyEvent, modifiersDown: Modifier*): Boolean = {
    val modifiersUp: Seq[Modifier] = Modifier.complementOf(modifiersDown)
    hasModifiers(ke, modifiersDown: _*) && hasntModifiers(ke, modifiersUp: _*)
  }

  def hasModifiers(ke: KeyEvent, modifiers: Modifier*): Boolean = hasModifiers(ke, true, modifiers: _*)
  def hasntModifiers(ke: KeyEvent, modifiers: Modifier*): Boolean = hasModifiers(ke, false, modifiers: _*)

  def hasModifiers(ke: KeyEvent, down: Boolean, modifiers: Modifier*): Boolean =
    modifiers.forall(mod => if (down) hasModifier(ke, mod)else !hasModifier(ke, mod))

  def hasModifier(ke: KeyEvent, modifier: Modifier): Boolean =
    modifier match {
      case Alt => ke.altDown
      case Ctrl => ke.controlDown
      case Shift => ke.shiftDown
      case Meta => ke.metaDown
      case Shortcut => ke.shortcutDown
    }
}
