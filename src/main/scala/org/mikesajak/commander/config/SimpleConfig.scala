package org.mikesajak.commander.config

/**
  * Created by mike on 09.04.17.
  */
class SimpleConfig extends Configuration {
  private var settingsMap = Map[String, String]()

  override def boolProperty(name: String): Option[Boolean] = settingsMap.get(name).map(_.toBoolean)

  override def boolProperty(name: String, value: Boolean): Unit = {
    // TODO:
  }

  override def intProperty(name: String): Option[Int] = settingsMap.get(name).map(_.toInt)

  override def intProperty(name: String, value: Int): Unit = {
    // TODO:
  }

  override def stringProperty(name: String): Option[String] = settingsMap.get(name)

  override def stringProperty(name: String, value: String): Unit = {
    // TODO:
  }
}
