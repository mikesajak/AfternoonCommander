package org.mikesajak.commander.config

import com.google.common.eventbus.Subscribe
import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.EventBus

import scala.language.implicitConversions

case class ConfigKey(category: String, name: String) {
  override def toString = s"$category.$name"
}

object ConfigKey {
  implicit def toConfigKey(tuple: (String, String)): ConfigKey = ConfigKey(tuple._1, tuple._2)

  val Any = ConfigKey("*", "*")
  def forCategory(category: String) = ConfigKey(category, "*")

  def matches(workingKey: ConfigKey, registrationKey: ConfigKey): Boolean = {
    val key = registrationKey.toString
    key == "*" || key == "*.*" ||
      key == workingKey.toString ||
      key == workingKey.category || key == s"${workingKey.category}.*"
  }

}

/**
  * Created by mike on 09.04.17.
  */
abstract class Configuration(eventBus: EventBus) { config =>
  private var observers = Map[String, List[ConfigObserver]]()

  abstract class PropertyOperator[A] (key: ConfigKey) {
    def value: Option[A]
    def getOrElse(altValue: => A): A = value.getOrElse(altValue)
    def :=(value: A): Unit

    override def toString: String = s"($key, $value)"
  }

  class StringPropertyOperator(key: ConfigKey) extends PropertyOperator[String](key) {
    def value: Option[String] = config.getStringProperty(key)
    def :=(value: String): Unit = config.setStringProperty(key, value)
  }

  class BoolPropertyOperator(key: ConfigKey) extends PropertyOperator[Boolean](key) {
    def value: Option[Boolean] = config.getBoolProperty(key)
    def :=(value: Boolean): Unit = config.setBoolProperty(key, value)
  }

  class IntPropertyOperator(key: ConfigKey)  extends PropertyOperator[Int](key) {
    def value: Option[Int] = config.getIntProperty(key)
    def :=(value: Int): Unit = config.setIntProperty(key, value)
  }

  class StringSeqPropertyOperator(key: ConfigKey)  extends PropertyOperator[Seq[String]](key) {
    def value: Option[Seq[String]] = config.getStringSeqProperty(key)
    def :=(value: Seq[String]): Unit = config.setStringSeqProperty(key, value)
  }

  def stringProperty(category: String, name: String): PropertyOperator[String] = stringProperty(ConfigKey(category, name))
  def stringProperty(key: ConfigKey): PropertyOperator[String] = new StringPropertyOperator(key)

  def boolProperty(category: String, name: String): PropertyOperator[Boolean] = boolProperty(ConfigKey(category, name))
  def boolProperty(key: ConfigKey): PropertyOperator[Boolean] = new BoolPropertyOperator(key)

  def intProperty(category: String, name: String): PropertyOperator[Int] = intProperty(ConfigKey(category, name))
  def intProperty(key: ConfigKey): PropertyOperator[Int] = new IntPropertyOperator(key)

  def stringSeqProperty(category: String, name: String): PropertyOperator[Seq[String]] = stringSeqProperty(ConfigKey(category, name))
  def stringSeqProperty(key: ConfigKey): PropertyOperator[Seq[String]] = new StringSeqPropertyOperator(key)


  protected def getBoolProperty(key: ConfigKey): Option[Boolean]
  protected def setBoolProperty(key: ConfigKey, value: Boolean): Unit

  protected def getIntProperty(key: ConfigKey): Option[Int]
  protected def setIntProperty(key: ConfigKey, value: Int): Unit

  protected def getStringProperty(key: ConfigKey): Option[String]
  protected def setStringProperty(key: ConfigKey, value: String): Unit

  protected def getStringSeqProperty(key: ConfigKey): Option[Seq[String]]
  protected def setStringSeqProperty(key: ConfigKey, value: Seq[String])

  def notifyConfigChanged(key: ConfigKey): Unit = {
    val paths = List(key.toString, key.category, "*", "*.*")

    paths.flatMap(observers.get(_))
      .flatten
      .foreach(o => o.configChanged(key))

    eventBus.publish(key)
  }

  def save()
  def load()
}

trait ConfigObserver {
  def observedKey: ConfigKey

  @Subscribe
  def filteredConfigChange(key: ConfigKey): Unit = {
    if (ConfigKey.matches(key, observedKey))
      configChanged(key)
  }

  def configChanged(key: ConfigKey): Unit
}

class LoggingConfigObserver(config: Configuration) {
  private val logger = Logger[SimpleConfig]

  @Subscribe
  def logConfigChange(key: ConfigKey): Unit = {
    val path = key.toString
    logger.info(s"Configuration changed: $path -> ${config.stringProperty(key)}")
  }
}
