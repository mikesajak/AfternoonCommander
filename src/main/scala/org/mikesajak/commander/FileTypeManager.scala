package org.mikesajak.commander

import org.mikesajak.commander.fs.local.{SymlinkDir, SymlinkFile}
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}

/**
  * Created by mike on 22.04.17.
  */
class FileTypeManager(archiveManager: ArchiveManager) {
  private val defaultFileTypeDetector = new DefaultFileTypeDetector
  private var fileTypeDetectors = List[FileTypeDetector](archiveManager)
  private var handlersMap = Map[FileType, FileHandler]()

  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("jpg", "jpeg", "png", "gif"), GraphicFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("avi", "mkv", "mov", "mpg", "mpv"), VideoFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector("txt", TextFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector("pdf", PdfFile))

  def registerFileTypeDetector(detector: FileTypeDetector): Unit =
    fileTypeDetectors ::= detector

  def detectFileType(path: VPath): FileType = {

    def detect(detectors: Seq[FileTypeDetector]): Option[FileType] = {
      var remaining = detectors
      while (remaining.nonEmpty) {
        val ft = remaining.head.detect(path)
        if (ft.isDefined) return ft
        remaining = remaining.tail
      }
      None
    }

    detect(fileTypeDetectors)
      .orElse(defaultFileTypeDetector.detect(path))
      .getOrElse(OtherFile)
  }

  def registerFileTypeHandler(fileType: FileType, fileHandler: FileHandler): Unit =
    handlersMap += fileType -> fileHandler

  def fileTypeHandler(path: VPath): Option[FileHandler] =
    detectFileType(path) match {
      case OtherFile => None
      case t @ _ => handlersMap.get(t)
    }
}

sealed abstract class FileType(icon: Option[IconDef]) {
  def this(icon: IconDef) = this(Some(icon))
  def this(iconName: String) = this(IconDef(iconName))
  def this() = this(None)

  private def nameForSize(size: Int) = icon.map(i => s"${i.name}-$size.${i.ext}")

  def smallIcon: Option[String] = nameForSize(24)
  def mediumIcon: Option[String] = nameForSize(36)
  def bigIcon: Option[String] = nameForSize(48)
}

case class IconDef(name: String, ext: String = "png", small: Boolean = true, medium: Boolean = true, big: Boolean = true)

case object ExecutableFile extends FileType("open-in-app")
case object SymbolicLinkFile extends FileType("link-variant")
case object TextFile extends FileType("note-text")
case object GraphicFile extends FileType("file-image")
case object VideoFile extends FileType("file-video")
case object ArchiveFile extends FileType("archive")
case object DirectoryType extends FileType("folder")
case object ParentDirectoryType extends FileType("arrow-left-thick")
case object PdfFile extends FileType("file-pdf")
case object OtherFile extends FileType()

trait FileTypeDetector {
  def detect(path: VPath): Option[FileType]
}

class DefaultFileTypeDetector extends FileTypeDetector {
  override def detect(path: VPath): Option[FileType] = path match {
    case d: PathToParent => Some(ParentDirectoryType)
    case s: SymlinkFile => Some(SymbolicLinkFile)
    case s: SymlinkDir => Some(SymbolicLinkFile)
    case d: VDirectory=> Some(DirectoryType)
//    case f: VFile if f.attribs contains 'x' => Some(ExecutableFile)
    case _ => None
  }
}

class SimpleByExtensionFileDetector(extensions: Seq[String], fileType: FileType) extends FileTypeDetector {
  def this(extension: String, fileType: FileType) = this(List(extension), fileType)

  override def detect(path: VPath): Option[FileType] = path match {
    case p if p.isFile =>
      val f = p.asInstanceOf[VFile]
      f.extension.flatMap(ext => if (extensions.contains(ext)) Some(fileType) else None)
    case _ => None
  }
}

trait FileHandler {
  def handle(path: VPath)
}
