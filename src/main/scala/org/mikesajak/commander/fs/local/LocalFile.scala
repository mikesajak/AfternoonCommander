package org.mikesajak.commander.fs.local

import java.io._
import java.nio.file.{Files, Paths}
import java.time.Instant

import org.mikesajak.commander.fs.{VDirectory, VFile, VFileUpdater}

import scala.util.Try

class LocalFile(inputFile: File, override val fileSystem: LocalFS) extends LocalPath with VFile {

  override val file: File = Paths.get(inputFile.getAbsolutePath).toFile

  override def inStream = new BufferedInputStream(Files.newInputStream(Paths.get(inputFile.getAbsolutePath)))

  override lazy val updater: Option[VFileUpdater] = Some(new LocalFileUpdater(this))

  override def exists: Boolean = file.exists()

  override def canEqual(other: Any): Boolean = other.isInstanceOf[LocalFile]
}

class LocalFileUpdater(file: LocalFile) extends VFileUpdater {
  override def delete(): Try[Boolean] = Try {
    val fsPath = Paths.get(file.absolutePath)
    Files.deleteIfExists(fsPath)
  }

  override def create(): Try[Boolean] = Try {
    val fsPath = Paths.get(file.absolutePath)
    val parentFsPath = fsPath.getParent
    Files.createDirectories(parentFsPath)
    Files.createFile(fsPath)
    true
  }

  override def move(targetDir: VDirectory, name: Option[String]): Try[Boolean] = Try {
    targetDir match {
      case targetLocalDir: LocalDirectory =>
        val targetPath = Paths.get(targetDir.absolutePath, name.getOrElse(file.name))
        file.file.renameTo(targetPath.toFile)
      case _ => false
    }
  }

  override def setModificationDate(date: Instant): Boolean =
    file.file.setLastModified(date.getEpochSecond)

  override def outStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(file.file.getAbsolutePath)))

  case class FileMoveException(message: String) extends Exception(message) {
    def this(message: String, cause: Exception) = {
      this(message)
      initCause(cause)
    }
    def this(cause: Exception) = this(null, cause)
  }
}
