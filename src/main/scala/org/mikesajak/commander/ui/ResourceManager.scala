package org.mikesajak.commander.ui

import scalafx.scene.image.Image

/**
  * Created by mike on 23.04.17.
  */
class ResourceManager {
  def getIcon(name: String) = {
    new Image(s"/images/$name")
  }

  def getIcon(name: String, width: Int, height: Int) = {
    new Image(s"/images/$name", width, height, true, true)
  }
}
