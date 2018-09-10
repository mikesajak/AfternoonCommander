package org.mikesajak.commander

import java.awt.Desktop
import java.util.concurrent.Executors

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.OSType.Windows
import org.mikesajak.commander.fs._
import org.mikesajak.commander.fs.local.LocalFile

import scala.concurrent.{ExecutionContext, Future}

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
  private var handlersMap = Map[FileType, FileHandler]()
  private val defaultFileHandler = new DefaultFileHandler(appController)

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
      case OtherFile => Some(defaultFileHandler)//None
      case t @ _ => handlersMap.get(t).orElse(Some(defaultFileHandler))
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

class DefaultFileHandler(appCtrl: ApplicationController) extends FileHandler {
  val logger = Logger[DefaultFileHandler]

  private implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  override def handle(path: VPath): Unit = {
    if (Desktop.isDesktopSupported) path match {
      case file: LocalFile => Future {
          try {
            logger.debug(s"Executing default (defined in desktop) action for file $file")
            Desktop.getDesktop.open(file.file)
            logger.debug(s"Finished Executing default (defined in desktop) action for file $file")
          } catch {
            case e: Exception => logger.warn(s"An error occurred while executing (desktop) action for file $file")
          }
        }

      case file: VFile => logger.info(s"The file $file is not a local file, skipping desktop action.")

      case dir: VDirectory => logger.info(s"The file $dir is a directory. Skipping desktop action.")

      case file@_ => logger.info(s"Unknown file $file. Skipping desktop action.")
    }
    else logger.warn(s"Cannot execute default action for $path. Desktop action is not supported by OS.")
  }
}
