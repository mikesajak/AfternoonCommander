package org.mikesajak.commander

import org.apache.tika.Tika
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import org.mikesajak.commander.OSType.Windows
import org.mikesajak.commander.archive.ArchiveManager
import org.mikesajak.commander.fs._
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.TextUtil.camelToSnake

sealed abstract class FileType(val icon: Option[String]) {
  def this(icon: String) = this(Some(icon))
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

  abstract class SourceCodeFile(override val icon: Option[String]) extends FileType(icon) {
    def this(icon: String) = this(Some(icon))
  }

  case object GenericSourceCodeFile extends SourceCodeFile("icons8-source-code-64.png")
  case object ScalaSourceCodeFile extends SourceCodeFile("Scala.png")
  case object JavaSourceCodeFile extends SourceCodeFile("icons8-java-50.png")
  case object CppSourceCodeFile extends SourceCodeFile("icons8-c++-50.png")
  case object CSourceCodeFile extends SourceCodeFile("icons8-c-programming-64.png")
  case object PythonSourceCodeFile extends SourceCodeFile("icons8-python-50.png")

  case object LogFile extends FileType("icons8-log-48.png")
  case object PropertiesFile extends FileType("icons8-view-details-50.png")
  case object ShellScript extends FileType("icons8-console-90.png")

  case object OtherFile extends FileType("file-outline.png")
}

class FileTypeManager(archiveManager: ArchiveManager, osResolver: OSResolver,
                      resourceMgr: ResourceManager, appController: ApplicationController) {
  import FileType._

  private val defaultFileTypeDetector = new DefaultFileTypeDetector
  private var fileTypeDetectors = List[FileTypeDetector]()
  registerFileTypeDetector(archiveManager)
  registerFileTypeDetector(SimpleByExtensionFileDetector(ArchiveFile,
                                                         "zip", "tar", "gz", "tgz", "bz2", "tbz2", "7z", "rar"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(GraphicFile,
                                                         "jpg", "jpeg", "png", "gif", "tif", "tiff"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(VideoFile,
                                                         "avi", "mkv", "mov", "mpg", "mpv", "mp4",
                                                         "mpeg", "flv", "m4v", "m2v"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(MusicFile,
                                                         "mp3", "ogg", "wav", "wmv", "mpc", "m4a", "flac"))

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
  registerFileTypeDetector(SimpleByExtensionFileDetector(PresentationFile, "odp"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(GenericSourceCodeFile,
                                                         "net",
                                                         "kt",
                                                         "groovy",
                                                         "html", "htm", "css",
                                                         "xml", "json",
                                                         "yml", "yaml"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(ScalaSourceCodeFile, "scala", "sbt"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(JavaSourceCodeFile, "java"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(CppSourceCodeFile, "cpp", "c++", "cxx", "hpp"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(CSourceCodeFile, "c", "h"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(PythonSourceCodeFile, "py"))

  registerFileTypeDetector(SimpleByExtensionFileDetector(LogFile, "log"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(PropertiesFile, "properties"))
  registerFileTypeDetector(SimpleByExtensionFileDetector(ShellScript, "sh", "bat"))

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

  def descriptionOf(fileType: FileType): String = {
    resourceMgr.getMessageOpt(s"file_type_manager.${camelToSnake(fileType.toString)}")
               .getOrElse(fileType.toString)
  }

  def mimeTypeOf(path: VPath): String = {
    val tika = new Tika()
    path match {
      case _: VDirectory => "application/x-directory"
      case f: VFile => tika.detect(f.inStream)
    }
  }

  def readMetadataOf(file: VFile): Map[String, Seq[String]] = {
    val meta = if (file.exists) {
      val contentHandler = new BodyContentHandler(Int.MaxValue)
      val metadata = new Metadata()
      metadata.set(TikaCoreProperties.ORIGINAL_RESOURCE_NAME, file.name)
      val parser = new AutoDetectParser()
      parser.parse(file.inStream, contentHandler, metadata)

      metadata.names()
              .map(name => (name, metadata.getValues(name).toSeq))
    } else Array[(String, Seq[String])]()

    (meta :+ ("name" -> Seq(file.name))
          :+ ("type" -> Seq("file")))
        .toMap
  }

  def metadataOf(path: VPath): Map[String, Seq[String]] = {
    path match {
      case f: VFile => readMetadataOf(f)
      case d: VDirectory =>
        List("name" -> Seq(d.name),
             "type" -> Seq("directory")).toMap
    }
  }

  private val execExtensions = Set("exe", "bat", "cmd")
  def isExecutable(path: VPath): Boolean = {
    val execAttrib = path.attributes.contains(Attrib.Executable) && !path.attributes.contains(Attrib.Directory)

    if (osResolver.getOSType != Windows) execAttrib
    else {
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
    case _: PathToParent => Some(FileType.ParentDirectoryType)
//    case s: SymlinkFile => Some(FileType.SymbolicLinkType)
//    case s: SymlinkDir => Some(FileType.SymbolicLinkType)
    case _: VDirectory=> Some(FileType.DirectoryType)
//    case f: VFile if f.attribs contains 'x' => Some(ExecutableFile)
    case _ => None
  }
}

case class SimpleByExtensionFileDetector(fileType: FileType, extensions: Set[String]) extends FileTypeDetector {
  override def detect(path: VPath): Option[FileType] = path match {
    case p if p.isFile =>
      val f = p.asInstanceOf[VFile]
      f.extension.flatMap(ext => if (extensions.contains(ext.toLowerCase)) Some(fileType) else None)
    case _ => None
  }
}

object SimpleByExtensionFileDetector {
  def apply(fileType: FileType, extensions: String*) = new SimpleByExtensionFileDetector(fileType, Set(extensions:_*))
}
