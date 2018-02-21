package org.mikesajak.commander.ui

import java.text.MessageFormat
import java.util.{Locale, ResourceBundle}

import com.typesafe.scalalogging.Logger

import scalafx.scene.image.Image

/**
  * Created by mike on 23.04.17.
  */
class ResourceManager {
  def getIcon(name: String): Image = try {
    new Image(s"/images/$name")
  } catch {
    case e: Exception =>
      Logger[ResourceManager].warn(s"Exception thrown during getting icon $name", e)
      throw e
  }

  def getIcon(name: String, width: Int, height: Int): Image = try {
    new Image(s"/images/$name", width, height, true, true)
  } catch {
    case e: Exception =>
      Logger[ResourceManager].warn(s"Exception thrown during getting icon $name, width=$width, height=$height", e)
      throw e
  }

  def getMessage(key: String, resourceFile: String = "ui", locale: Locale = Locale.getDefault()): String =
    ResourceBundle.getBundle(resourceFile).getString(key)

  def getMessageWithArgs(key: String, args: Seq[Any],
                         resourceFile: String = "ui", locale: Locale = Locale.getDefault): String = {
    val pattern = getMessage(key, resourceFile)
    val formatter = new MessageFormat("")
    formatter.setLocale(locale)
    formatter.applyPattern(pattern)
    formatter.format(args.toArray)
  }
}
