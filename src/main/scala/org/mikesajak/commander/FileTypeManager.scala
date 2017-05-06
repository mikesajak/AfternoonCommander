package org.mikesajak.commander

import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}

import scala.annotation.tailrec

/**
  * Created by mike on 22.04.17.
  */
class FileTypeManager(archiveManager: ArchiveManager) {
  private var fileTypeDetectors = List[FileTypeDetector](new DefaultFileTypeDetector(),
                                                         archiveManager)
  private var handlersMap = Map[FileType, FileHandler]()
  private var iconsMap = Map[FileType, String]()

  registerIcon(DirectoryType, "ic_folder_black_24dp_1x.png")
  registerIcon(ParentDirectoryType, "ic_arrow_back_black_24dp_1x.png")
  registerIcon(GraphicFile, "ic_image_black_24dp_1x.png")
  registerIcon(ArchiveFile, "ic_business_center_black_24dp_1x.png")

  def registerFileTypeDetector(detector: FileTypeDetector): Unit =
    fileTypeDetectors ::= detector

  def detectFileType(path: VPath): FileType = {
    @tailrec
    def detect(detectors: List[FileTypeDetector]): FileType =
      detectors match {
        case Nil => OtherFile
        case detector :: tail =>
          val fileType = detector.detect(path)
          if (fileType.isDefined) fileType.get
          else detect(tail)
      }

    detect(fileTypeDetectors)
  }

  def registerFileTypeHandler(fileType: FileType, fileHandler: FileHandler): Unit =
    handlersMap += fileType -> fileHandler

  def fileTypeHandler(path: VPath): Option[FileHandler] =
    detectFileType(path) match {
      case OtherFile => None
      case t @ _ => handlersMap.get(t)
    }

  def registerIcon(fileType: FileType, iconName: String): Unit =
    iconsMap += fileType -> iconName

  def getIcon(fileType: FileType): Option[String] = {
    val ft =
      fileType match {
        case ArchiveFile => ArchiveFile // TODO: some better method mapping different archive types to single type
        case _ => fileType
      }
    iconsMap.get(ft)
  }

}

sealed trait FileType
trait ArchiveFile extends FileType

case object ExecutableFile extends FileType
case object TextFile extends FileType
case object GraphicFile extends FileType
case object VideoFile extends FileType
case object ArchiveFile extends ArchiveFile
case object DirectoryType extends FileType
case object ParentDirectoryType extends FileType
case object OtherFile extends FileType

trait FileTypeDetector {
  def detect(path: VPath): Option[FileType]
}

class DefaultFileTypeDetector extends FileTypeDetector {
  override def detect(path: VPath): Option[FileType] = path match {
    case d: PathToParent => Some(ParentDirectoryType)
    case d: VDirectory=> Some(DirectoryType)
    case f: VFile if f.attribs contains('x')=> Some(ExecutableFile)
    case _ => None
  }
}

trait FileHandler {
  def handle(path: VPath)
}
