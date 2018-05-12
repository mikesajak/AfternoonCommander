package org.mikesajak.commander.ui

import java.util.{Locale, ResourceBundle}

import com.ibm.icu.text.MessageFormat
import com.typesafe.scalalogging.Logger
import scalafx.scene.image.Image

import scala.language.implicitConversions

sealed abstract class IconSize(val size: Int)

object IconSize {
  case object Small extends IconSize(24)
  case object Medium extends IconSize(36)
  case object Big extends IconSize(48)
}

/**
  * Created by mike on 23.04.17.
  */
class ResourceManager {
  def getIcon(name: String, size: Option[IconSize]): Image = {
    val path = s"/images/$name"
    size.map(s => getIcon(path, s)).getOrElse(getIcon2(path))
  }

  def getIcon(name: String, size: IconSize): Image =
    getIcon(name, size.size, size.size)

  def getIcon2(name: String): Image = {
    val imagePath = s"/images/$name"
    try {
      new Image(imagePath)
    } catch {
      case e: Exception =>
        Logger[ResourceManager].warn(s"Exception thrown during getting icon $imagePath", e)
        throw e
    }
  }

  def getIcon(name: String, width: Int, height: Int): Image = {
    val imagePath = s"/images/$name"
    try {
      new Image(imagePath, width, height, true, true)
    } catch {
      case e: Exception =>
        Logger[ResourceManager].warn(s"Exception thrown during getting icon $imagePath, width=$width, height=$height", e)
        throw e
    }
  }

  def getMessage(key: String, resourceFile: String = "ui", locale: Locale = Locale.getDefault()): String =
    ResourceBundle.getBundle(resourceFile).getString(key)

  def getMessageOpt(key: String, resourceFile: String = "ui", locale: Locale = Locale.getDefault()): Option[String] =
    if (ResourceBundle.getBundle(resourceFile).containsKey(key))
      Some(getMessage(key, resourceFile, locale))
    else None


  def getMessageWithArgs(key: String, args: Seq[Any],
                         resourceFile: String = "ui", locale: Locale = Locale.getDefault): String = {
    val pattern = getMessage(key, resourceFile)
    val formatter = new MessageFormat("")
    formatter.setLocale(locale)
    formatter.applyPattern(pattern)
    formatter.format(args.toArray)
  }
}
