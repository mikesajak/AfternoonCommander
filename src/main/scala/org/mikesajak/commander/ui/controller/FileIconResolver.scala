package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.{Attrib, VPath}
import org.mikesajak.commander.ui.ResourceManager
import scalafx.scene.effect.BlendMode
import scalafx.scene.image.ImageView
import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle
import scalafx.scene.{CacheHint, Group, Node}

class FileIconResolver(fileTypeMgr: FileTypeManager,
                       resourceMgr: ResourceManager) {

  def findIconFor(path: VPath): Option[Node] = {
    var icon = findIcon(path)
    if (fileTypeMgr.isExecutable(path))
      icon = icon.map(i => new Group(i, execOverlayIcon))
    if (path.attributes.contains(Attrib.Symlink))
      icon = icon.map(i => new Group(i, symlinkOverlayIcon))
    icon
  }

  private def findIcon(path: VPath): Option[Node] = {
    val fileType = fileTypeMgr.detectFileType(path)
    fileType.icon.map { iconFile =>
      val imageView = new ImageView(resourceMgr.getIcon(iconFile, 18, 18))
      imageView.preserveRatio = true
      imageView.cache = true
      imageView.cacheHint = CacheHint.Speed
      imageView
    }
  }

  private def execOverlayIcon: Node =
    createOverlayBadge(10, 11, 10, Color.DarkGreen, "asterisk-light.png")

  private def symlinkOverlayIcon =
    createOverlayBadge(0, 11, 10, Color.DarkBlue, "icons8-right-2-48.png")

  private def createOverlayBadge(posX: Double, posY: Double, size: Double, color: Color,
                                 iconName: String) = {
    val icon = new ImageView(resourceMgr.getIcon(iconName, size, size))
    icon.cache = true
    icon.cacheHint = CacheHint.Speed
    icon.x = posX
    icon.y = posY
    icon.blendMode = BlendMode.SrcAtop
    val rad = size / 2 + 0.5
    new Group(circle(posX + rad, posY + rad, rad, color), icon)
  }

  private def circle(posX: Double, posY: Double, radius0: Double, color: Color) =
    new Circle {
      radius = radius0
      centerX = posX
      centerY = posY
      fill = color
    }
}
