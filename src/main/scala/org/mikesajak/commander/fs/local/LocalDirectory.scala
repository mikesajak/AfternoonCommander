package org.mikesajak.commander.fs.local

import java.io.File
import java.nio.file.{Files, Paths}
import java.time.Instant

import org.mikesajak.commander.fs.{VDirectory, VDirectoryUpdater, VPath}

import scala.util.Try

/**
 * Created by mike on 26.10.14.
 */
class LocalDirectory(override val file: File, override val fileSystem: LocalFS)
    extends LocalPath with VDirectory {

  override def childFiles: Seq[LocalFile] = {
    val files = file.listFiles()
    if (files != null)
      files.filter(f => !f.isDirectory).map(resolveFile)
    else Seq()
  }

  override def isParent(path: VPath): Boolean =
    path.absolutePath startsWith absolutePath

  private def resolveFile(file: File) = {
    if (Files.isSymbolicLink(file.toPath)) new SymlinkFile(file, fileSystem)
    else new LocalFile(file, fileSystem)
  }

  override def childDirs: Seq[LocalDirectory] ={
    val files = file.listFiles()
    if (files != null)
      files.filter(f => f.isDirectory).map(resolveDir)
    else Seq()
  }

  private def resolveDir(file: File) = {
    if (Files.isSymbolicLink(file.toPath)) new SymlinkDir(file, fileSystem)
    else new LocalDirectory(file, fileSystem)
  }

  override def exists: Boolean = file.exists()

  override lazy val updater: Option[LocalDirectoryUpdater] = Some(new LocalDirectoryUpdater(this))

  override def canEqual(other: Any): Boolean = other.isInstanceOf[LocalDirectory]
}

class LocalDirectoryUpdater(dir: LocalDirectory) extends VDirectoryUpdater {
  override def mkChildDirPath(child: String): LocalDirectory =
    new LocalDirectory(new File(dir.file, child), dir.fileSystem)

  override def mkChildFilePath(child: String): LocalFile =
    new LocalFile(new File(dir.file.getAbsolutePath + File.separator + child), dir.fileSystem)

  override def setModificationDate(date: Instant): Boolean =
    dir.file.setLastModified(date.toEpochMilli)

  override def delete(): Try[Boolean] = Try {
    val fsPath = Paths.get(dir.absolutePath)
    Files.deleteIfExists(fsPath)
  }

  override def create(): Try[Boolean] = Try {
    dir.file.mkdir()
  }

  override def move(targetDir: VDirectory, targetName: Option[String]): Try[Boolean] = Try {
    targetDir match {
      case targetLocalDir: LocalDirectory =>
        val targetPath = Paths.get(targetDir.absolutePath, targetName.getOrElse(dir.name))
        dir.file.renameTo(targetPath.toFile)
      case _ => false
    }
  }

  case class DirectoryMoveException(message: String) extends Exception(message) {
    def this(message: String, cause: Exception) = {
      this(message)
      initCause(cause)
    }
    def this(cause: Exception) = this(null, cause)
  }
}
