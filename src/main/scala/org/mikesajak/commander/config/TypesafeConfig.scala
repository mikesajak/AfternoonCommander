package org.mikesajak.commander.config

import com.typesafe.config._
import org.mikesajak.commander.EventBus
import scribe.Logging

import java.io.{File, FileWriter}
import scala.jdk.CollectionConverters._

/**
  * Created by mike on 17.04.17.
  */
class TypesafeConfig(filename: String, eventBus: EventBus)
    extends Configuration(eventBus) with Logging {

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

  override def entriesKeys: Seq[String] = {
    config.entrySet().asScala
        .map(e => e.getKey)
        .map(key => key.replaceFirst(s"^$appName\\.", ""))
        .toSeq
  }

  override def getBoolProperty(key: String): Option[Boolean] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getBoolean(path)) else None
  }

  override def setBoolProperty(key: String, value: Boolean): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromAnyRef(value))
    notifyConfigChanged(key)
  }

  override def getIntProperty(key: String): Option[Int] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getInt(path)) else None
  }

  override def setIntProperty(key: String, value: Int): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromAnyRef(value))
    notifyConfigChanged(key)
  }

  override def getStringProperty(key: String): Option[String] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getString(path)) else None
  }

  override def setStringProperty(key: String, value: String): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromAnyRef(value))
    notifyConfigChanged(key)
  }

  override def getStringSeqProperty(key: String): Option[Seq[String]] = {
    val path = s"$appName.$key"
    if (config.hasPath(path)) Some(config.getStringList(path).asScala.toSeq) else None
  }

  override def setStringSeqProperty(key: String, value: Seq[String]): Unit = {
    config = config.withValue(s"$appName.$key", ConfigValueFactory.fromIterable(value.asJava))
    notifyConfigChanged(key)
  }
}
