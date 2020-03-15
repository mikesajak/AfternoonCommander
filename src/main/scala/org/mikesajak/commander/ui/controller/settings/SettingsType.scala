package org.mikesajak.commander.ui.controller.settings

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class SettingsType extends EnumEntry
object SettingsType extends Enum[SettingsType] {
  val values: immutable.IndexedSeq[SettingsType] = findValues

  case object IntType extends SettingsType
  case object BoolType extends SettingsType
  case object StringType extends SettingsType
  case object ColorType extends SettingsType
  case object ExecFileType extends SettingsType

  def parseString(text: String): SettingsType = text match {
    case "int" => IntType
    case "bool" => BoolType
    case "string" => StringType
    case "color" => ColorType
    case "file.exec" => ExecFileType
  }
}

