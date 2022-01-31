package org.mikesajak.commander.config

import com.google.common.eventbus.Subscribe
import org.mikesajak.commander.EventBus
import scribe.Logging

import java.util.regex.Pattern
import scala.language.implicitConversions

/**
  * Created by mike on 09.04.17.
  */
abstract class Configuration(eventBus: EventBus) { config =>
  def entriesKeys: Seq[String]

  def stringProperty(category: String, name: String): PropertyOperator[String] = stringProperty(s"$category.$name")
  def stringProperty(key: String): PropertyOperator[String] = new StringPropertyOperator(key)

  def boolProperty(category: String, name: String): PropertyOperator[Boolean] = boolProperty(s"$category.$name")
  def boolProperty(key: String): PropertyOperator[Boolean] = new BoolPropertyOperator(key)

  def intProperty(category: String, name: String): PropertyOperator[Int] = intProperty(s"$category.$name")
  def intProperty(key: String): PropertyOperator[Int] = new IntPropertyOperator(key)

  def stringSeqProperty(category: String, name: String): PropertyOperator[Seq[String]] = stringSeqProperty(s"$category.$name")
  def stringSeqProperty(key: String): PropertyOperator[Seq[String]] = new StringSeqPropertyOperator(key)


  protected def getBoolProperty(key: String): Option[Boolean]
  protected def setBoolProperty(key: String, value: Boolean): Unit

  protected def getIntProperty(key: String): Option[Int]
  protected def setIntProperty(key: String, value: Int): Unit

  protected def getStringProperty(key: String): Option[String]
  protected def setStringProperty(key: String, value: String): Unit

  protected def getStringSeqProperty(key: String): Option[Seq[String]]
  protected def setStringSeqProperty(key: String, value: Seq[String]): Unit

  def notifyConfigChanged(key: String): Unit =
    eventBus.publish(key)

  def save(): Unit
  def load(): Unit

  abstract class PropertyOperator[A] (key: String) {
    def value: Option[A]
    def getOrElse(altValue: => A): A = value.getOrElse(altValue)
    def :=(value: A): Unit

    override def toString: String = s"($key, $value)"
  }

  class StringPropertyOperator(key: String) extends PropertyOperator[String](key) {
    def value: Option[String] = config.getStringProperty(key)
    def :=(value: String): Unit = config.setStringProperty(key, value)
  }

  class BoolPropertyOperator(key: String) extends PropertyOperator[Boolean](key) {
    def value: Option[Boolean] = config.getBoolProperty(key)
    def :=(value: Boolean): Unit = config.setBoolProperty(key, value)
  }

  class IntPropertyOperator(key: String)  extends PropertyOperator[Int](key) {
    def value: Option[Int] = config.getIntProperty(key)
    def :=(value: Int): Unit = config.setIntProperty(key, value)
  }

  class StringSeqPropertyOperator(key: String)  extends PropertyOperator[Seq[String]](key) {
    def value: Option[Seq[String]] = config.getStringSeqProperty(key)
    def :=(value: Seq[String]): Unit = config.setStringSeqProperty(key, value)
  }
}

trait ConfigObserver {
  val observedKeyPrefix: String
  private lazy val predicate = {
    val key = if (observedKeyPrefix.startsWith("runtime.")) observedKeyPrefix.substring("runtime.".length)
              else observedKeyPrefix
    val escapedKey = Pattern.quote(if (key.endsWith(".")) key else s"$key.")
    Pattern.compile(s"^(:?$escapedKey|runtime\\.$escapedKey)").asPredicate()
  }

  //noinspection UnstableApiUsage
  @Subscribe
  def filteredConfigChange(key: String): Unit = {
    if (predicate.test(key))
      configChanged(key)
  }

  def configChanged(key: String): Unit
}

class LoggingConfigObserver(config: Configuration) extends Logging {

  //noinspection UnstableApiUsage
  @Subscribe
  def logConfigChange(key: String): Unit = {
    logger.info(s"Configuration changed: $key -> ${config.stringProperty(key)}")
  }
}
