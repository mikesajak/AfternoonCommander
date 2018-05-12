package org.mikesajak.commander

import org.mikesajak.commander.fs.local.{SymlinkDir, SymlinkFile}
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}

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

object FileType { // TODO: i18 (note: file types are resolved dynamically, by type name)
  case object ExecutableFile extends FileType("open-in-app")
  case object SymbolicLinkType extends FileType("link-variant")
  case object TextFile extends FileType("note-text")
  case object GraphicFile extends FileType("file-image")
  case object VideoFile extends FileType("file-video")
  case object MusicFile extends FileType("file-music")
  case object ArchiveFile extends FileType("archive")
  case object DirectoryType extends FileType("folder")
  case object ParentDirectoryType extends FileType("arrow-left-thick")
  case object PdfFile extends FileType("file-pdf")
  case object WordFile extends FileType("file-word")
  case object DocumentFile extends FileType("file-document")
  case object ExcelFile extends FileType("file-excel")
  case object SpreadsheetFile extends FileType("file-chart")
  case object DelimitedFile extends FileType("file-delimited")
  case object PowerpointFile extends FileType("file-powerpoint")
  case object PresentationFile extends FileType("file-presentation-box")
  case object XmlFile extends FileType("file-xml")
  case object EbookFile extends FileType("book-open-variant")
  case object OtherFile extends FileType("file-outline")
}

class FileTypeManager(archiveManager: ArchiveManager) {

  import FileType._

  private val defaultFileTypeDetector = new DefaultFileTypeDetector
  private var fileTypeDetectors = List[FileTypeDetector]()
  private var handlersMap = Map[FileType, FileHandler]()

//  registerFileTypeDetector(archiveManager)
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("zip", "tar", "gz", "tgz", "bz2", "tbz2", "7z", "rar"), ArchiveFile))

  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("jpg", "jpeg", "png", "gif"), GraphicFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("avi", "mkv", "mov", "mpg", "mpv", "mp4"), VideoFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("mp3", "ogg", "wav"), MusicFile))

  registerFileTypeDetector(new SimpleByExtensionFileDetector("xml", XmlFile))

  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("doc", "docx"), WordFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("odt"), DocumentFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector("pdf", PdfFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("txt"), TextFile))

  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("epub", "mobi"), EbookFile))

  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("xls", "xlsx"), ExcelFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("ods"), SpreadsheetFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("csv", "tsv"), DelimitedFile))

  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("ppt", "pptx"), PowerpointFile))
  registerFileTypeDetector(new SimpleByExtensionFileDetector(List("odp", "pptx"), PresentationFile))

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

trait FileTypeDetector {
  def detect(path: VPath): Option[FileType]
}

class DefaultFileTypeDetector extends FileTypeDetector {

  override def detect(path: VPath): Option[FileType] = path match {
    case d: PathToParent => Some(FileType.ParentDirectoryType)
    case s: SymlinkFile => Some(FileType.SymbolicLinkType)
    case s: SymlinkDir => Some(FileType.SymbolicLinkType)
    case d: VDirectory=> Some(FileType.DirectoryType)
//    case f: VFile if f.attribs contains 'x' => Some(ExecutableFile)
    case _ => None
  }
}

class SimpleByExtensionFileDetector(extensions: Seq[String], fileType: FileType) extends FileTypeDetector {
  def this(extension: String, fileType: FileType) = this(List(extension), fileType)

  override def detect(path: VPath): Option[FileType] = path match {
    case p if p.isFile =>
      val f = p.asInstanceOf[VFile]
      f.extension.flatMap(ext => if (extensions.contains(ext.toLowerCase)) Some(fileType) else None)
    case _ => None
  }
}

trait FileHandler {
  def handle(path: VPath)
}
