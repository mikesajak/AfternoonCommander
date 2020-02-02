package org.mikesajak.commander.ui.controller.settings

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.ResourceManager

trait SettingsItem {
  val name: String
  val category: String
  val description: String
  val itemType: Class[_]
  def value: Any
  def value_=(valueToSet: Any)
  def updateConfigValue(): Unit
  def isUpdated: Boolean
}

case class SettingsGroup(name: String, items: Seq[SettingsItem], childGroups: Seq[SettingsGroup])

class SettingsItemFactory(config: Configuration, resourceMgr: ResourceManager) {
  private val logger = Logger[SettingsItemFactory]
  private var configValue: Any = _
  private var updated: Boolean = false

  private def configKeys: (Seq[String], Seq[String]) = {
    val (runtimeKeys, persistentKeys) = config.entriesKeys
                                              .sorted
                                              .partition(_.startsWith("runtime."))
    println(s"runtime: $runtimeKeys")
    println(s"persistent: $persistentKeys")

    (runtimeKeys, persistentKeys)
  }

  def settingsItems: Seq[SettingsItem] = configKeys._2.map(key => mkSettingsItem(key))
  def settingsCategories: Seq[String] = {
    configKeys._2.map(key => resourceMgr.getMessageOpt(s"config.$key")
                                        .map(description => description.split("#")(1))
                                        .getOrElse(key))
  }
  def settingsItemCategoryMap: Map[String, Seq[SettingsItem]] = {
    configKeys._2.map(key => mkSettingsItem(key))
                 .groupBy(item => item.category)
  }

  def settingsGroups: Seq[SettingsGroup] = {
    configKeys._2.map(key => mkSettingsItem(key))
        .groupBy(item => item.category)
        .map { case (category, items) => new SettingsGroup(category, items, Seq.empty) }
        .toSeq
  }

  def mkSettingsItem(key: String): SettingsItem = {
    new SettingsItem() {
      override val category: String = resourceMgr.getMessageOpt(s"config.$key")
                                                 .map(description => description.split("#")(1))
                                                 .getOrElse(key)

      override val name: String = resourceMgr.getMessageOpt(s"config.$key")
                                             .map(description => description.split("#")(2))
                                             .getOrElse(key)

      override val description: String = resourceMgr.getMessageOpt(s"config.$key")
                                                    .map(description => description.split("#")(3))
                                                    .getOrElse(key)

      override val itemType: Class[_] = typeStr(key) match {
        case "string" => classOf[String]
        case "int" => classOf[Int]
        case "bool" => classOf[Boolean]
      }

      override def value: Any = typeStr(key) match {
        case "bool" => config.boolProperty(key).getOrElse(false)
        case "int"  => config.intProperty(key).getOrElse(0)
        case "string" => config.stringProperty(key).getOrElse("")
      }

      override def value_=(valueToSet: Any): Unit = {
        logger.debug(s"Settings item $key (of type $itemType) set to $valueToSet")
        configValue = valueToSet
        updated = true
      }

      override def updateConfigValue(): Unit = {
        val valueType = typeStr(key)
        logger.info(s"Setting config $key (of type $valueType) to $value")

        valueType match {
          case "bool" => config.boolProperty(key) := value.asInstanceOf[Boolean]
          case "int" => config.intProperty(key) := value.asInstanceOf[Int]
          case "string" => config.stringProperty(key) := value.toString
        }
      }

      override def isUpdated: Boolean = updated
    }
  }

  private def typeStr(key: String) =
    resourceMgr.getMessageOpt(s"config.$key")
               .map(description => description.split("#")(0))
               .getOrElse("string")
}
