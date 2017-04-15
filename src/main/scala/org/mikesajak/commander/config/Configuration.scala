package org.mikesajak.commander.config

/**
  * Created by mike on 09.04.17.
  */
trait Configuration {
  def getBoolSetting(name: String): Option[Boolean]
  def getIntSetting(name: String): Option[Integer]
  def getStringSetting(name: String): Option[String]
}
