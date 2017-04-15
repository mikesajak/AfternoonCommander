package org.mikesajak.commander.config

/**
  * Created by mike on 09.04.17.
  */
class SimpleConfig extends Configuration {
  private var settingsMap = Map[String, String]()

  override def getBoolSetting(name: String): Option[Boolean] = settingsMap.get(name).map(_.toBoolean)

  override def getIntSetting(name: String): Option[Integer] = settingsMap.get(name).map(_.toInt)

  override def getStringSetting(name: String): Option[String] = settingsMap.get(name)
}
