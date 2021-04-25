package org.mikesajak.commander.ui.controller.settings

import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.controller.settings.SettingsType._
import scalafx.scene.paint.Color
import scribe.Logging

abstract class SettingsItem(key: String, val itemType: SettingsType, settingsDataProvider: SettingsDataProvider)
    extends Logging {
  
  val name: String = settingsDataProvider.itemName(key)
  val category: String = settingsDataProvider.itemCategory(key)
  val description: String = settingsDataProvider.itemDescription(key)
  val order: Int = settingsDataProvider.itemOrder(key)

  def getConfigValue: Any
  def updateConfigValue(newValue: Any): Unit
}

class StringSettingsItem(key: String, configKey: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, StringType, settingsDataProvider) {

  override def getConfigValue: String = settingsDataProvider.config.stringProperty(configKey).getOrElse("")
  override def updateConfigValue(newValue: Any): Unit = {
    logger.info(s"Setting config $configKey (of type $itemType) to $newValue")
    settingsDataProvider.config.stringProperty(configKey) := newValue.asInstanceOf[String]
  }
}

class IntSettingsItem(key: String, configKey: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, IntType, settingsDataProvider) {

  override def getConfigValue: Int = settingsDataProvider.config.intProperty(configKey).getOrElse(0)
  override def updateConfigValue(newValue: Any): Unit = {
    logger.info(s"Setting config $configKey (of type $itemType) to $newValue")
    settingsDataProvider.config.intProperty(configKey) := newValue.asInstanceOf[Int]
  }
}

class BoolSettingsItem(key: String, configKey: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, BoolType, settingsDataProvider) {

  override def getConfigValue: Boolean = settingsDataProvider.config.boolProperty(configKey).getOrElse(false)
  override def updateConfigValue(newValue: Any): Unit = {
    logger.info(s"Setting config $configKey (of type $itemType) to $newValue")
    settingsDataProvider.config.boolProperty(configKey) := newValue.asInstanceOf[Boolean]
  }
}

class ColorSettingsItem(key: String, configKey: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, ColorType, settingsDataProvider) {

  override def getConfigValue: Color =
    Color.web(settingsDataProvider.config.stringProperty(configKey).getOrElse("#000000"))

  override def updateConfigValue(newValue: Any): Unit = {
    val strValue = convertToString(newValue.asInstanceOf[Color])
    logger.info(s"Setting config $configKey (of type $itemType) to $strValue")
    settingsDataProvider.config.stringProperty(configKey) := strValue
  }

  private def convertToString(color: Color): String = {
    s"#${format(color.red)}${format(color.green)}${format(color.blue)}"
  }

  private def format(value: Double) = {
    val str = math.round(value * 255).toInt.toHexString
    if (str.length == 0) s"0$str" else str
  }
}

class ExecFileSettingsItem(key: String, configKey: String, settingsDataProvider: SettingsDataProvider)
    extends SettingsItem(key, ExecFileType, settingsDataProvider) {

  override def getConfigValue: String = settingsDataProvider.config.stringProperty(configKey).getOrElse("")
  override def updateConfigValue(newValue: Any): Unit = {
    logger.info(s"Setting config $configKey (of type $itemType) to $newValue")
    settingsDataProvider.config.stringProperty(configKey) := newValue.asInstanceOf[String]
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

