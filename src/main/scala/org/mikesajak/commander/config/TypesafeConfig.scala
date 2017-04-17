package org.mikesajak.commander.config

import java.io.FileWriter

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}
import com.typesafe.scalalogging.Logger

/**
  * Created by mike on 17.04.17.
  */
class TypesafeConfig(filename: String) extends Configuration {

  private val logger = Logger[TypesafeConfig]
  private var config = ConfigFactory.load(filename)

  override def load(): Unit = {
    logger.info(s"Loading settings from $filename")
    config = ConfigFactory.load(filename)
  }

  override def save(): Unit = {
    logger.info(s"Saving settings to $filename")
    val opts = ConfigRenderOptions.defaults()
      .setOriginComments(false)
      .setFormatted(true)
      .setFormatted(true)
      .setJson(false)
    val contents = config.getConfig("app").root.render(opts)
    val writer = new FileWriter(filename)
    writer.write(contents)
    writer.close()
  }

  override def boolProperty(name: String): Option[Boolean] = {
    val path = s"app.$name"
    if (config.hasPath(path)) Some(config.getBoolean(path)) else None
  }

  override def boolProperty(name: String, value: Boolean): Unit =
    config = config.withValue(s"app.$name", ConfigValueFactory.fromAnyRef(value))

  override def intProperty(name: String): Option[Int] = {
    val path = s"app.$name"
    if (config.hasPath(path)) Some(config.getInt(path)) else None
  }

  override def intProperty(name: String, value: Int): Unit =
    config = config.withValue(s"app.$name", ConfigValueFactory.fromAnyRef(value))

  override def stringProperty(name: String): Option[String] = {
    val path = s"app.$name"
    if (config.hasPath(path)) Some(config.getString(path)) else None
  }

  override def stringProperty(name: String, value: String): Unit =
    config = config.withValue(s"app.$name", ConfigValueFactory.fromAnyRef(value))
}
