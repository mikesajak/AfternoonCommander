package org.mikesajak.commander.config

import com.typesafe.scalalogging.Logger

/**
  * Created by mike on 09.04.17.
  */
class SimpleConfig extends Configuration {
  private val logger = Logger[SimpleConfig]
  private var settingsMap = Map[String, String]()

  private def updateValue(key: ConfigKey, value: String): Unit = {
    val path = key.toString
    settingsMap += path -> value

    notifyObservers(key)
  }

  override def getBoolProperty(key: ConfigKey): Option[Boolean] =
    settingsMap.get(key.toString).map(_.toBoolean)
  override def setBoolProperty(key: ConfigKey, value: Boolean): Unit =
    updateValue(key, value.toString)

  override def getIntProperty(key: ConfigKey): Option[Int] =
    settingsMap.get(key.toString).map(_.toInt)
  override def setIntProperty(key: ConfigKey, value: Int): Unit =
    updateValue(key, value.toString)

  override def getStringProperty(key: ConfigKey): Option[String] =
    settingsMap.get(key.toString)
  override def setStringProperty(key: ConfigKey, value: String): Unit =
    updateValue(key, value)

  override def getStringSeqProperty(key: ConfigKey): Option[Seq[String]] =
//    settingsMap.get(key.toString)
    throw new UnsupportedOperationException

  override def setStringSeqProperty(key: ConfigKey, value: Seq[String]): Unit =
    throw new UnsupportedOperationException

  override def load(): Unit = {
    logger.warn("Config load not implemented")
  }

  override def save(): Unit = {
    logger.warn("Config save not implemented")
  }
}
