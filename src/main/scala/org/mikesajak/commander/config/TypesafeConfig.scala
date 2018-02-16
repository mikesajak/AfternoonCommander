package org.mikesajak.commander.config

import java.io.{File, FileWriter}

import com.typesafe.config._
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

/**
  * Created by mike on 17.04.17.
  */
class TypesafeConfig(filename: String) extends Configuration {

  private val logger = Logger[TypesafeConfig]
  private var config: Config = ConfigFactory.empty("Afternoon Commander settings")

  private val appName = "afternooncommander"

  override def load(): Unit = {
    logger.info(s"Loading settings from $filename")
    config = ConfigFactory.parseFile(new File(filename))
    config = config.withOnlyPath(appName)
    logger.debug(s"Current config:\n${renderConfig()}")
  }

  def renderConfig(): String = {
    val opts = ConfigRenderOptions.defaults()
      .setOriginComments(false)
      .setFormatted(true)
      .setJson(false)
    config.withOnlyPath(appName).root.render(opts)
  }

  override def save(): Unit = {
    logger.info(s"Saving settings to $filename")
    val contents = renderConfig()
    val writer = new FileWriter(filename)
    writer.write(contents)
    writer.close()
  }

  override def boolProperty(category: String, name: String): Option[Boolean] = {
    val path = s"$appName.$category.$name"
    if (config.hasPath(path)) Some(config.getBoolean(path)) else None
  }

  override def setProperty(category: String, name: String, value: Boolean): Unit = {
    config = config.withValue(s"$appName.$category.$name", ConfigValueFactory.fromAnyRef(value))
    notifyObservers(category, name)
  }

  override def intProperty(category: String, name: String): Option[Int] = {
    val path = s"$appName.$category.$name"
    if (config.hasPath(path)) Some(config.getInt(path)) else None
  }

  override def setProperty(category: String, name: String, value: Int): Unit = {
    config = config.withValue(s"$appName.$category.$name", ConfigValueFactory.fromAnyRef(value))
    notifyObservers(category, name)
  }

  override def stringProperty(category: String, name: String): Option[String] = {
    val path = s"$appName.$category.$name"
    if (config.hasPath(path)) Some(config.getString(path)) else None
  }

  override def setStringProperty(category: String, name: String, value: String): Unit = {
    config = config.withValue(s"$appName.$category.$name", ConfigValueFactory.fromAnyRef(value))
    notifyObservers(category, name)
  }

  def stringSeqProperty(category: String, name: String): Option[Seq[String]] = {
    val path = s"$appName.$category.$name"
    if (config.hasPath(path)) Some(config.getStringList(path).asScala) else None
  }

  def setStringSeqProperty(category: String, name: String, value: Seq[String]): Unit = {
    config = config.withValue(s"$appName.$category.$name", ConfigValueFactory.fromIterable(value.asJava))
    notifyObservers(category, name)
  }
}
