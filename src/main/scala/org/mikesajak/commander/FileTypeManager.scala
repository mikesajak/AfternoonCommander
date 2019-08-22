package org.mikesajak.commander

import org.mikesajak.commander.OSType.Windows
import org.mikesajak.commander.archive.ArchiveManager
import org.mikesajak.commander.fs._

sealed abstract class FileType(val icon: Option[String]) {
  def this(icon: String) = this(Some(icon))
  def this() = this(None)
}

object FileType {
  case object ExecutableFile extends FileType("open-in-app.png")
  case object SymbolicLinkType extends FileType("link-variant.png")
  case object TextFile extends FileType("note-text.png")
  case object GraphicFile extends FileType("file-image.png")
  case object VideoFile extends FileType("file-video.png")
  case object MusicFile extends FileType("file-music.png")
  case object ArchiveFile extends FileType("archive.png")
  case object DirectoryType extends FileType("folder.png")
  case object ParentDirectoryType extends FileType("arrow-left-thick.png")
  case object PdfFile extends FileType("file-pdf.png")
  case object WordFile extends FileType("file-word.png")
  case object DocumentFile extends FileType("file-document.png")
  case object ExcelFile extends FileType("file-excel.png")
  case object SpreadsheetFile extends FileType("file-chart.png")
  case object DelimitedFile extends FileType("file-delimited.png")
  case object PowerpointFile extends FileType("file-powerpoint.png")
  case object PresentationFile extends FileType("file-presentation-box.png")
  case object XmlFile extends FileType("file-xml.png")
  case object EbookFile extends FileType("book-open-variant.png")
  case object OtherFile extends FileType("file-outline.png")
}

class FileTypeManager(archiveManager: ArchiveManager, osResolver: OSResolver,
                      appController: ApplicationController) {
  import FileType._

  private val defaultFileTypeDetector = new DefaultFileTypeDetector
  private var fileTypeDetectors = List[FileTypeDetector]()
  registerFileTypeDetector(archiveManager)
  registerFileTypeDetector(SimpleByExtensionFileDetector(ArchiveFile, "zip", "tar", "gz", "tgz", "bz2", "tbz2", "7z", "rar"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(GraphicFile, "jpg", "jpeg", "png", "gif"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(VideoFile, "avi", "mkv", "mov", "mpg", "mpv", "mp4"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(MusicFile, "mp3", "ogg", "wav"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(XmlFile, "xml"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(WordFile, "doc", "docx"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(DocumentFile, "odt"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(PdfFile, "pdf"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(TextFile, "txt"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(EbookFile, "epub", "mobi"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(ExcelFile, "xls", "xlsx"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(SpreadsheetFile, "ods"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(DelimitedFile, "csv", "tsv"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(PowerpointFile, "ppt", "pptx"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(PresentationFile, "odp", "pptx"))

  def registerFileTypeDetector(detector: FileTypeDetector): Unit =
    fileTypeDetectors ::= detector

  def detectFileType(path: VPath): FileType = {
    val maybeFileType = fileTypeDetectors.view
                                         .map(_.detect(path))
                                         .collectFirst { case Some(x) => x }

    maybeFileType
      .orElse(defaultFileTypeDetector.detect(path))
      .getOrElse(OtherFile)
  }

  def isExecutable(path: VPath): Boolean = {
    val execAttrib = path.attributes.contains(Attrib.Executable) && !path.attributes.contains(Attrib.Directory)

    if (osResolver.getOSType != Windows) execAttrib
    else {
      val execExtensions = List("exe", "bat", "cmd")
      val knownExtension = path match {
        case f: VFile if f.extension.isDefined => execExtensions.contains(f.extension.get)
        case _ => false
      }
      execAttrib && knownExtension
    }
  }
}

trait FileTypeDetector {
  def detect(path: VPath): Option[FileType]
}

class DefaultFileTypeDetector extends FileTypeDetector {

  override def detect(path: VPath): Option[FileType] = path match {
    case d: PathToParent => Some(FileType.ParentDirectoryType)
//    case s: SymlinkFile => Some(FileType.SymbolicLinkType)
//    case s: SymlinkDir => Some(FileType.SymbolicLinkType)
    case d: VDirectory=> Some(FileType.DirectoryType)
//    case f: VFile if f.attribs contains 'x' => Some(ExecutableFile)
    case _ => None
  }
}

case class SimpleByExtensionFileDetector(fileType: FileType, extensions: String*) extends FileTypeDetector {
  override def detect(path: VPath): Option[FileType] = path match {
    case p if p.isFile =>
      val f = p.asInstanceOf[VFile]
      f.extension.flatMap(ext => if (extensions.contains(ext.toLowerCase)) Some(fileType) else None)
    case _ => None
  }
}
