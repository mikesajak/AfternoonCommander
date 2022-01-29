package org.mikesajak.commander.ui

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.ibm.icu.text.MessageFormat
import scalafx.scene.image.Image
import scribe.Logging

import java.util.{Locale, ResourceBundle}
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
class ResourceManager extends Logging {

  private case class ImageKey(path: String, size: Option[(Double, Double)] = None)
  private val imageCache = CacheBuilder.newBuilder().build[ImageKey, Image](new CacheLoader[ImageKey, Image] {
    override def load(key: ImageKey): Image = mkImage(key.path, key.size)
  })

  private case class MessageKey(key: String, locale: Locale, file: String)
  private val messageCache = CacheBuilder.newBuilder().build[MessageKey, String](new CacheLoader[MessageKey, String] {
    override def load(key: MessageKey): String = getMessageImpl(key.key, key.locale, key.file)
  })

  private def getImage(key: ImageKey) = {
    imageCache.get(key)
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
      getImage(ImageKey(imagePath))
    } catch {
      case e: Exception =>
        logger.warn(s"Exception thrown during getting icon $imagePath", e)
        throw e
    }
  }

  def getIcon(name: String, width: Double, height: Double): Image = {
    val imagePath = s"/images/$name"
    try {
      getImage(ImageKey(imagePath, Some((width, height))))
    } catch {
      case e: Exception =>
        logger.warn(s"Exception thrown during getting icon $imagePath, width=$width, height=$height", e)
        throw e
    }
  }

  def getMessage(key: String, locale: Locale = Locale.getDefault())(implicit resourceFile: String = "ui"): String =
    messageCache.get(MessageKey(key, locale, resourceFile))

  private def getMessageImpl(key: String, locale: Locale, resourceFile: String) =
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
