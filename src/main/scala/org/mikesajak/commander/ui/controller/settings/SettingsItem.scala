package org.mikesajak.commander.ui.controller.settings

import com.typesafe.scalalogging.Logger
import enumeratum.{Enum, EnumEntry}
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.controller.settings.SettingsType.{BoolType, ColorType, ExecFileType, IntType, StringType}
import scalafx.scene.paint.Color

import scala.collection.immutable

abstract class SettingsItem(key: String, val itemType: SettingsType,
                            settingsDataProvider: SettingsDataProvider) {
  val logger: Logger = Logger[SettingsItem]

  protected var configValue: Any = _
  protected var updated: Boolean = false

  val name: String = settingsDataProvider.itemName(key)
  val category: String = settingsDataProvider.itemCategory(key)
  val description: String = settingsDataProvider.itemDescription(key)
  val order: Int = settingsDataProvider.itemOrder(key)

  def value_=(valueToSet: Any): Unit = {
    logger.debug(s"Settings item $key (of type $itemType) set to $valueToSet")
    configValue = valueToSet
    updated = true
  }

  def value: Any
  def updateConfigValue(): Unit

  def isUpdated: Boolean = updated
}

class StringSettingsItem(key: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, StringType, settingsDataProvider) {

  override def value: String = settingsDataProvider.config.stringProperty(key).getOrElse("")
  override def updateConfigValue(): Unit = {
    logger.info(s"Setting config $key (of type $itemType) to $value")
    settingsDataProvider.config.stringProperty(key) := value
  }
}

class IntSettingsItem(key: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, IntType, settingsDataProvider) {

  override def value: Int = settingsDataProvider.config.intProperty(key).getOrElse(0)
  override def updateConfigValue(): Unit = {
    logger.info(s"Setting config $key (of type $itemType) to $value")
    settingsDataProvider.config.intProperty(key) := value
  }
}

class BoolSettingsItem(key: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, BoolType, settingsDataProvider) {

  override def value: Boolean = settingsDataProvider.config.boolProperty(key).getOrElse(false)
  override def updateConfigValue(): Unit = {
    logger.info(s"Setting config $key (of type $itemType) to $value")
    settingsDataProvider.config.boolProperty(key) := value
  }
}

class ColorSettingsItem(key: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, ColorType, settingsDataProvider) {

  override def value: Color =
    Color.web(settingsDataProvider.config.stringProperty(key).getOrElse("#000000"))
  override def updateConfigValue(): Unit = {
    val strValue = convertToString(value)
    logger.info(s"Setting config $key (of type $itemType) to $strValue")
    settingsDataProvider.config.stringProperty(key) := strValue
  }

  private def convertToString(color: Color): String = {
    s"#${format(color.red)}${format(color.green)}${format(color.blue)}"
  }

  private def format(value: Double) = {
    val str = math.round(value * 255).toInt.toHexString
    if (str.length == 0) s"0$str" else str
  }
}

class ExecFileSettingsItem(key: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, ExecFileType, settingsDataProvider) {

  override def value: String = settingsDataProvider.config.stringProperty(key).getOrElse("")
  override def updateConfigValue(): Unit = {
    logger.info(s"Setting config $key (of type $itemType) to $value")
    settingsDataProvider.config.stringProperty(key) := value
  }
}

trait SettingsDataProvider {
  def itemName(key: String): String
  def itemCategory(key: String): String
  def itemDescription(key: String): String
  def itemTypeStr(key: String): String
  def itemOrder(key: String): Int

  def config: Configuration
}


case class SettingsGroup(name: String, items: Seq[SettingsItem], childGroups: Seq[SettingsGroup])

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
