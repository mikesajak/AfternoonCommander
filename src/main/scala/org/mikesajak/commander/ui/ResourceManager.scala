package org.mikesajak.commander.ui

import com.typesafe.scalalogging.Logger

import scalafx.scene.image.Image

/**
  * Created by mike on 23.04.17.
  */
class ResourceManager {
  def getIcon(name: String) = try {
    new Image(s"/images/$name")
  } catch {
    case e: Exception =>
      Logger[ResourceManager].warn(s"Exception thrown during getting icon $name", e)
      throw e
  }

  def getIcon(name: String, width: Int, height: Int) = try {
    new Image(s"/images/$name", width, height, true, true)
  } catch {
    case e: Exception =>
      Logger[ResourceManager].warn(s"Exception thrown during getting icon $name, width=$width, height=$height", e)
      throw e
  }
}
