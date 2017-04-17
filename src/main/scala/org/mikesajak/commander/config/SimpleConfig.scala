package org.mikesajak.commander.config

import com.typesafe.scalalogging.Logger

/**
  * Created by mike on 09.04.17.
  */
class SimpleConfig extends Configuration {
  private val logger = Logger[SimpleConfig]
  private var settingsMap = Map[String, String]()

  override def boolProperty(name: String): Option[Boolean] = settingsMap.get(name).map(_.toBoolean)

  override def boolProperty(name: String, value: Boolean): Unit = {
    logger.info(s"Setting bool property $name=$value")
    settingsMap += name -> value.toString
  }

  override def intProperty(name: String): Option[Int] = settingsMap.get(name).map(_.toInt)

  override def intProperty(name: String, value: Int): Unit = {
    logger.info(s"Setting int property $name=$value")
    settingsMap += name -> value.toString
  }

  override def stringProperty(name: String): Option[String] = settingsMap.get(name)

  override def stringProperty(name: String, value: String): Unit = {
    logger.info(s"Setting string property $name=$value")
    settingsMap += name -> value
  }

  override def load() = {
    logger.warn("Config load not implemented")
  }

  override def save(): Unit = {
    logger.warn("Config save not implemented")
  }


}
