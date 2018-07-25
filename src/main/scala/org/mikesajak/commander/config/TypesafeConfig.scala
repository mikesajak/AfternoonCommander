package org.mikesajak.commander.config

import java.io.{File, FileWriter}

import com.typesafe.config._
import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.EventBus

import scala.collection.JavaConverters._

/**
  * Created by mike on 17.04.17.
  */
class TypesafeConfig(filename: String, eventBus: EventBus) extends Configuration(eventBus) {

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

  override def getBoolProperty(key: ConfigKey): Option[Boolean] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getBoolean(path)) else None
  }

  override def setBoolProperty(key: ConfigKey, value: Boolean): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromAnyRef(value))
    notifyConfigChanged(key)
  }

  override def getIntProperty(key: ConfigKey): Option[Int] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getInt(path)) else None
  }

  override def setIntProperty(key: ConfigKey, value: Int): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromAnyRef(value))
    notifyConfigChanged(key)
  }

  override def getStringProperty(key: ConfigKey): Option[String] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getString(path)) else None
  }

  override def setStringProperty(key: ConfigKey, value: String): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromAnyRef(value))
    notifyConfigChanged(key)
  }

  override def getStringSeqProperty(key: ConfigKey): Option[Seq[String]] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getStringList(path).asScala) else None
  }

  override def setStringSeqProperty(key: ConfigKey, value: Seq[String]): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromIterable(value.asJava))
    notifyConfigChanged(key)
  }
}
