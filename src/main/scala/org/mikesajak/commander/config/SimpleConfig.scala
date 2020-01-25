package org.mikesajak.commander.config

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.EventBus

/**
  * Created by mike on 09.04.17.
  */
class SimpleConfig(eventBus: EventBus) extends Configuration(eventBus) {
  private val logger = Logger[SimpleConfig]
  private var settingsMap = Map[String, String]()

  override def entriesKeys: Seq[String] = settingsMap.keys.toSeq

  override def getBoolProperty(key: String): Option[Boolean] =
    settingsMap.get(key).map(_.toBoolean)
  override def setBoolProperty(key: String, value: Boolean): Unit =
    updateValue(key, value.toString)

  override def getIntProperty(key: String): Option[Int] =
    settingsMap.get(key).map(_.toInt)
  override def setIntProperty(key: String, value: Int): Unit =
    updateValue(key, value.toString)

  override def getStringProperty(key: String): Option[String] =
    settingsMap.get(key)
  override def setStringProperty(key: String, value: String): Unit =
    updateValue(key, value)

  private def updateValue(key: String, value: String): Unit = {
    val path = key
    settingsMap += path -> value

    notifyConfigChanged(key)
  }

  override def getStringSeqProperty(key: String): Option[Seq[String]] =
//    settingsMap.get(key.toString)
    throw new UnsupportedOperationException

  override def setStringSeqProperty(key: String, value: Seq[String]): Unit =
    throw new UnsupportedOperationException

  override def load(): Unit = {
    logger.warn("Config load not implemented")
  }

  override def save(): Unit = {
    logger.warn("Config save not implemented")
  }
}
