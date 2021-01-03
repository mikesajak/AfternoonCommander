package org.mikesajak.commander.ui

import java.util.{Locale, ResourceBundle}

import com.ibm.icu.text.MessageFormat
import com.typesafe.scalalogging.Logger
import scalafx.scene.image.Image

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

sealed abstract class IconSize(val size: Double)

object IconSize {
  def apply(size: Double): IconSize = new IconSize(size) {}
  case object Tiny extends IconSize(16)
  case object Small extends IconSize(24)
  case object Medium extends IconSize(36)
  case object Big extends IconSize(48)
}

/**
  * Created by mike on 23.04.17.
  */
class ResourceManager {

  private case class CacheKey(path: String, size: Option[(Double, Double)])
  private object CacheKey {
    def apply(path: String): CacheKey = new CacheKey(path, None)
  }
  private var cache: Map[CacheKey, Image] = Map()

  private def getImage(key: CacheKey) = {
    if (!cache.contains(key)) {
      val image = mkImage(key.path, key.size)
      cache += key -> image
    }
    cache(key)
  }

  private def mkImage(path: String, size: Option[(Double, Double)]) =
    size.map(s => new Image(path, s._1, s._2, true, true))
      .getOrElse(new Image(path))

  def getIcon(name: String, size: Option[IconSize]): Image = {
    val path = s"/images/$name"
    size.map(s => getIcon(path, s)).getOrElse(getIcon(path))
  }

  def getIcon(name: String, size: IconSize): Image =
    getIcon(name, size.size, size.size)

  def getIcon(name: String): Image = {
    val imagePath = s"/images/$name"
    try {
      getImage(CacheKey(imagePath))
    } catch {
      case e: Exception =>
        Logger[ResourceManager].warn(s"Exception thrown during getting icon $imagePath", e)
        throw e
    }
  }

  def getIcon(name: String, width: Double, height: Double): Image = {
    val imagePath = s"/images/$name"
    try {
      getImage(CacheKey(imagePath, Some((width, height))))
    } catch {
      case e: Exception =>
        Logger[ResourceManager].warn(s"Exception thrown during getting icon $imagePath, width=$width, height=$height", e)
        throw e
    }
  }

  def getMessage(key: String, locale: Locale = Locale.getDefault())(implicit resourceFile: String = "ui"): String =
    ResourceBundle.getBundle(resourceFile, locale).getString(key)

  def getMessageOpt(key: String, locale: Locale = Locale.getDefault())(implicit resourceFile: String = "ui"): Option[String] =
    if (ResourceBundle.getBundle(resourceFile).containsKey(key))
      Some(getMessage(key, locale)(resourceFile))
    else None


  def getMessageWithArgs(key: String, args: Seq[Any], locale: Locale = Locale.getDefault)
                        (implicit resourceFile: String = "ui"): String = {
    val pattern = getMessage(key)(resourceFile)
    val formatter = new MessageFormat("")
    formatter.setLocale(locale)
    formatter.applyPattern(pattern)
    formatter.format(args.toArray)
  }

  def getKeys(prefix: Option[String] = None)(implicit resourceFile: String = "ui"): Seq[String] = {
    ResourceBundle.getBundle(resourceFile).getKeys.asScala
                  .filter(key => prefix.forall(p => key.startsWith(p)))
                  .toSeq
  }
}
