package org.mikesajak.commander.config

import com.typesafe.scalalogging.Logger

/**
  * Created by mike on 09.04.17.
  */
trait Configuration {
  private var observers = Map[String, List[ConfigObserver]]()

  def boolProperty(category: String, name: String): Option[Boolean]
  def setProperty(category: String, name: String, value: Boolean): Unit

  def intProperty(category: String, name: String): Option[Int]
  def setProperty(category: String, name: String, value: Int): Unit

  def stringProperty(category: String, name: String): Option[String]
  def setProperty(category: String, name: String, value: String): Unit

  def stringSeqProperty(category: String, name: String): Option[Seq[String]]
  def setProperty(category: String, name: String, value: Seq[String])


  def registerObserver(category: String, name: String, observer: ConfigObserver): Unit = {
    if (category == "*" && name != "*" || category != "*" && name == "*")
      throw new IllegalArgumentException(s"Partial wildcard not supported: category=$category, name=$name")
    
    val path = s"$category.$name"
    val curList: List[ConfigObserver] = observers.getOrElse(path, List[ConfigObserver]())
    observers += path -> (observer :: curList)
  }

  def unregisterObserver(category: String, name: String, observer: ConfigObserver): Unit = {
    val path = s"$category.$name"
    val valueObservers = observers.get(path)
    valueObservers.foreach { valObservers =>
      val newObserversList = valObservers.filter(_ != observer)
      observers += path -> newObserversList
    }
  }

  def notifyObservers(category: String, name: String): Unit = {
    val paths = List(s"$category.$name", category, "*", "*.*")

    paths.flatMap(observers.get(_))
      .flatten
      .foreach(o => o.configChanged(category, name))
  }

  def save()
  def load()
}

trait ConfigObserver {
  def configChanged(category: String, name: String)
}

class LoggingConfigObserver(config: Configuration) extends ConfigObserver {
  private val logger = Logger[SimpleConfig]

  override def configChanged(category: String, name: String): Unit = {
    val path = s"$category.$name"
    logger.info(s"Configuration changed: $path -> ${config.stringProperty(category, name)}")
  }
}
