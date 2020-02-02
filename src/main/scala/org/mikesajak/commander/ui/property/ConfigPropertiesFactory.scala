package org.mikesajak.commander.ui.property

import java.util.{Comparator, Optional}

import com.typesafe.scalalogging.Logger
import javafx.beans.value.ObservableValue
import org.controlsfx.control.PropertySheet
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.ResourceManager

class ConfigPropertiesFactory(config: Configuration, resourceMgr: ResourceManager) {

  private val logger = Logger[ConfigPropertiesFactory]

  def keys: (Seq[String], Seq[String]) = {
    val (runtimeKeys, persistentKeys) = config.entriesKeys
                                              .sorted
                                              .partition(_.startsWith("runtime."))
    println(s"runtime: $runtimeKeys")
    println(s"persistent: $persistentKeys")

    (runtimeKeys, persistentKeys)
  }

  private def typeStr(key: String) =
    resourceMgr.getMessageOpt(s"config.$key")
               .map(description => description.split("#")(0))
               .getOrElse("string")

  def item(key: String): PropertySheet.Item =
    new PropertySheet.Item {
      override def getType: Class[_] =
        typeStr(key) match {
          case "string" => classOf[String]
          case "int" => classOf[Int]
          case "bool" => classOf[Boolean]
        }

      override def getCategory: String =
        resourceMgr.getMessageOpt(s"config.$key")
                   .map(description => description.split("#")(1))
                   .getOrElse(key)

      override def getName: String =
        resourceMgr.getMessageOpt(s"config.$key")
                   .map(description => description.split("#")(2))
                   .getOrElse(key)

      override def getDescription: String =
        resourceMgr.getMessageOpt(s"config.$key")
                   .map(description => description.split("#")(3))
                   .getOrElse(key)

      override def getValue: AnyRef =
        typeStr(key) match {
          case "bool" => config.boolProperty(key).getOrElse(false).asInstanceOf[java.lang.Boolean]
          case "int"  => config.intProperty(key).getOrElse(0).intValue().asInstanceOf[java.lang.Integer]
          case "string" => config.stringProperty(key).getOrElse("")
        }

      override def setValue(value: AnyRef): Unit = {
        val valueType = typeStr(key)
        logger.info(s"Setting config $key (of type $valueType) to $value")

        valueType match {
          case "bool" => config.boolProperty(key) := value.asInstanceOf[Boolean]
          case "int" => config.intProperty(key) := value.asInstanceOf[Int]
          case "string" => config.stringProperty(key) := value.toString
        }
      }

      override def getObservableValue: Optional[ObservableValue[_]] = Optional.empty()
    }

  def createConfigItems(): Seq[PropertySheet.Item] = keys._2.map(key => item(key))

  def getCategoryOrderComparator: Comparator[String] = {
    val fixedOrderCategories = IndexedSeq("General", "UI")
    val categorySet = fixedOrderCategories.toSet

    new Comparator[String] {
      override def compare(cat1: String, cat2: String): Int = {
        if (categorySet.contains(cat1)) {
          if (categorySet.contains(cat2))
            fixedOrderCategories.indexOf(cat1) - fixedOrderCategories.indexOf(cat2)
          else -1
        }
        else if (categorySet.contains(cat2)) 1
        else cat1.compareTo(cat2)
      }
    }
  }
}