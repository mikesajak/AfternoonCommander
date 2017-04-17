package org.mikesajak.commander.config

/**
  * Created by mike on 09.04.17.
  */
trait Configuration {
  def boolProperty(name: String): Option[Boolean]
  def boolProperty(name: String, value: Boolean)
  def intProperty(name: String): Option[Int]
  def intProperty(name: String, value: Int)
  def stringProperty(name: String): Option[String]
  def stringProperty(name: String, value: String)

  def save()
  def load()
}
