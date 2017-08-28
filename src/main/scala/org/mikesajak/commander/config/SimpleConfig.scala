package org.mikesajak.commander.config

import com.typesafe.scalalogging.Logger

/**
  * Created by mike on 09.04.17.
  */
class SimpleConfig extends Configuration {
  private val logger = Logger[SimpleConfig]
  private var settingsMap = Map[String, String]()

  private def updateValue(category: String, name: String, value: String) = {
    val path = s"$category.$name"
    settingsMap += name -> value

    notifyObservers(category, name)
  }

  override def boolProperty(category: String, name: String): Option[Boolean] =
    settingsMap.get(s"$category.$name").map(_.toBoolean)
  override def setProperty(category: String, name: String, value: Boolean): Unit =
    updateValue(category, name, value.toString)

  override def intProperty(category: String, name: String): Option[Int] =
    settingsMap.get(s"$category.$name").map(_.toInt)
  override def setProperty(category: String, name: String, value: Int): Unit =
    updateValue(category, name, value.toString)

  override def stringProperty(category: String, name: String): Option[String] =
    settingsMap.get(s"$category.$name")
  override def setProperty(category: String, name: String, value: String): Unit =
    updateValue(category, name, value)

  override def stringSeqProperty(category: String, name: String): Option[Seq[String]] =
//    settingsMap.get(s"$category.$name")
    throw new UnsupportedOperationException

  override def setProperty(category: String, name: String, value: Seq[String]): Unit =
    throw new UnsupportedOperationException

  override def load(): Unit = {
    logger.warn("Config load not implemented")
  }

  override def save(): Unit = {
    logger.warn("Config save not implemented")
  }
}
