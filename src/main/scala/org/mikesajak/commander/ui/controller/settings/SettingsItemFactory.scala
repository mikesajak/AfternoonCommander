package org.mikesajak.commander.ui.controller.settings

import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.ui.controller.settings.SettingsType._

class SettingsItemFactory(config: Configuration, resourceMgr: ResourceManager) {
  private val configMetadataParser = new ConfigMetadataParser
  private val settingsDataProvider = new SettingsDataProviderImpl

  private implicit val configResourceFile: String = "config"

  private def configKeys: Seq[String] = resourceMgr.getKeys()

  def settingsItems: Seq[SettingsItem] = configKeys.map(key => mkSettingsItem(key))
  def settingsCategories: Seq[String] = {
    configKeys.map(key => resourceMgr.getMessageOpt(key)
                                     .map(configMetadataParser.extractCategory)
                                     .getOrElse(key))
  }
  def settingsItemCategoryMap: Map[String, Seq[SettingsItem]] = {
    configKeys.map(key => mkSettingsItem(key))
              .groupBy(item => item.category)
  }

  def settingsGroups: Seq[SettingsGroup] = {
    configKeys.map(key => mkSettingsItem(key))
              .groupBy(item => item.category)
              .map { case (category, items) => SettingsGroup(category, items.sortWith(itemComparator), Seq.empty) }
              .toSeq
  }

  private val itemComparator = (item1: SettingsItem, item2: SettingsItem) => {
    if (item1.order >= 0 && item2.order >= 0) item1.order < item2.order
    else if (item1.order >= 0) true
    else if (item2.order >= 0) false
    else item1.name.compareTo(item2.name) < 0
  }

  def mkSettingsItem(key: String): SettingsItem = {
    val configKey = key.replaceFirst("^config\\.", "")
    parseType(key) match {
      case IntType => new IntSettingsItem(key, configKey, settingsDataProvider)
      case BoolType => new BoolSettingsItem(key, configKey, settingsDataProvider)
      case ColorType => new ColorSettingsItem(key, configKey, settingsDataProvider)
      case StringType => new StringSettingsItem(key, configKey, settingsDataProvider)
      case ExecFileType => new ExecFileSettingsItem(key, configKey, settingsDataProvider)
    }
  }

  private def parseType(key: String) = SettingsType.parseString(typeStr(key))
  private def typeStr(key: String) = resourceMgr.getMessageOpt(key)
                                                .map(configMetadataParser.extractType)
                                                .getOrElse("string")

  class SettingsDataProviderImpl extends SettingsDataProvider {
    private val metadataParser = new ConfigMetadataParser

    def itemName(key: String): String = resourceMgr.getMessageOpt(key)
                                                   .map(metadataParser.extractName)
                                                   .getOrElse(key)

    private def categoryStr(key: String) = resourceMgr.getMessageOpt(key)
                                                      .map(metadataParser.extractCategory)
                                                      .getOrElse(key)

    private val categoryOrder = raw"(\S+?)(?:\[(\d+)\])?".r

    def itemCategory(key: String): String =
      categoryStr(key) match {
        case categoryOrder(category, _) => category
        case str @ _ => str
      }

    def itemOrder(key: String): Int = {
      val orderOpt = categoryStr(key) match {
        case categoryOrder(_, order) => Option(order)
        case _ => None
      }
      orderOpt.map(_.toInt).getOrElse(-1)
    }

    def itemDescription(key: String): String = resourceMgr.getMessageOpt(key)
                                                          .map(metadataParser.extractDescription)
                                                          .getOrElse(key)


    def itemTypeStr(key: String): String = resourceMgr.getMessageOpt(key)
                                                      .map(configMetadataParser.extractType)
                                                      .getOrElse("string")

    val config: Configuration = SettingsItemFactory.this.config
  }

  class ConfigMetadataParser {
    val separator = "/"

    def extractType(text: String): String = text.split(separator)(0)
    def extractCategory(text: String): String = text.split(separator)(1)
    def extractName(text: String): String = text.split(separator)(2)
    def extractDescription(text: String): String = text.split(separator)(3)
  }
}
