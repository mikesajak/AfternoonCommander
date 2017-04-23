package org.mikesajak.commander

import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}

import scala.annotation.tailrec

/**
  * Created by mike on 22.04.17.
  */
class FileTypeManager {
  private var fileTypeDetectors = List[FileTypeDetector](new DefaultFileTypeDetector())
  private var handlersMap = Map[FileType, FileHandler]()
  private var iconsMap = Map[FileType, String]()

  def registerFileTypeDetector(detector: FileTypeDetector): Unit =
    fileTypeDetectors ::= detector

  def detectFileType(path: VPath): FileType = {
    @tailrec
    def detect(detectors: List[FileTypeDetector]): FileType =
      detectors match {
        case Nil => OtherFile
        case detector :: tail =>
          val fileType = detector.detect(path)
          if (fileType != OtherFile) fileType
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

  def getIcon(fileType: FileType): Option[String] = iconsMap.get(fileType)

}

sealed trait FileType
case object ExecutableFile extends FileType
case object TextFile extends FileType
case object GraphicFile extends FileType
case object VideoFile extends FileType
case object DirectoryType extends FileType
case object ParentDirectoryType extends FileType
case object OtherFile extends FileType

trait FileTypeDetector {
  def detect(path: VPath): FileType
}

class DefaultFileTypeDetector extends FileTypeDetector {
  override def detect(path: VPath): FileType = path match {
    case d: PathToParent => ParentDirectoryType
    case d: VDirectory=> DirectoryType
    case f: VFile if f.attribs contains('x')=> ExecutableFile
    case _ => OtherFile
  }
}

trait FileHandler {
  def handle(path: VPath)
}
