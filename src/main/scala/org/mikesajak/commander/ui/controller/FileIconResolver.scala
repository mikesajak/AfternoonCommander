package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.archive.ArchiveManager
import org.mikesajak.commander.fs.{Attrib, VFile, VPath}
import org.mikesajak.commander.ui.ResourceManager
import scalafx.scene.effect.BlendMode
import scalafx.scene.image.ImageView
import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle
import scalafx.scene.{CacheHint, Group, Node}

class FileIconResolver(fileTypeMgr: FileTypeManager,
                       archiveManager: ArchiveManager,
                       resourceMgr: ResourceManager) {

  def findIconFor(path: VPath): Option[Node] = {
    findIcon(path)
        .map(i => addBottomLeftBadge(i, path))
        .map(i => addBottomRightBadge(i, path))
  }

  private def addBottomLeftBadge(icon: Node, path: VPath) = {
    path match {
      case f: VFile if archiveManager.findArchiveHandler(f).isDefined =>
        new Group(icon, archiveOverlayIconBottomRight)
      case f: VFile if fileTypeMgr.isExecutable(f) =>
        new Group(icon, execOverlayIconBottomRight)
      case _ => icon
    }
  }

  private def addBottomRightBadge(icon: Node, path: VPath) =
    path match {
      case p if p.attributes.contains(Attrib.Symlink) =>
        new Group(icon, symlinkOverlayIconBottomLeft)
      case _ => icon
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

  private def execOverlayIconBottomRight: Node =
    createOverlayBadgeBottomRight(Color.DarkGreen, "asterisk-light.png")

  private def archiveOverlayIconBottomRight: Node =
    createOverlayBadgeBottomRight(Color.DarkGreen, "icons8-package-48.png")

  private def symlinkOverlayIconBottomLeft =
    createOverlayBadgeBottomLeft(Color.DarkBlue, "icons8-right-2-48.png")

  private def createOverlayBadgeBottomRight(color: Color, iconName: String) =
    createOverlayBadge(0, 11, 10, color, iconName)

  private def createOverlayBadgeBottomLeft(color: Color, iconName: String) =
    createOverlayBadge(10, 11, 10, color, iconName)

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
