package org.mikesajak.commander.config

import com.typesafe.scalalogging.Logger

import scala.language.implicitConversions

case class ConfigKey(category: String, name: String) {
  override def toString = s"$category.$name"
}

object ConfigKey {
  implicit def toConfigKey(tuple: (String, String)): ConfigKey = ConfigKey(tuple._1, tuple._2)

  val Any = ConfigKey("*", "*")
  def forCategory(category: String) = ConfigKey(category, "*")
}

/**
  * Created by mike on 09.04.17.
  */
trait Configuration { config =>
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

  def registerObserver(observer: ConfigObserver): Unit = {
    val key = observer.observedKey
    if (key.category == "*" && key.name != "*" || key.category != "*" && key.name == "*")
      throw new IllegalArgumentException(s"Partial wildcard not supported: category=${key.category}, name=${key.name}")

    val path = key.toString
    val curList: List[ConfigObserver] = observers.getOrElse(path, List[ConfigObserver]())
    observers += path -> (observer :: curList)
  }

  def unregisterObserver(observer: ConfigObserver): Unit = {
    val path = observer.observedKey.toString
    val valueObservers = observers.get(path)
    valueObservers.foreach { valObservers =>
      val newObserversList = valObservers.filter(_ != observer)
      observers += path -> newObserversList
    }
  }

  def notifyObservers(key: ConfigKey): Unit = {
    val paths = List(key.toString, key.category, "*", "*.*")

    paths.flatMap(observers.get(_))
      .flatten
      .foreach(o => o.configChanged(key))
  }

  def save()
  def load()
}

trait ConfigObserver {
  def observedKey: ConfigKey
  def configChanged(key: ConfigKey)
}

class LoggingConfigObserver(override val observedKey: ConfigKey, config: Configuration) extends ConfigObserver {
  def this(config: Configuration) = this(ConfigKey.Any, config)

  private val logger = Logger[SimpleConfig]

  override def configChanged(key: ConfigKey): Unit = {
    val path = key.toString
    logger.info(s"Configuration changed: $path -> ${config.stringProperty(key)}")
  }
}
