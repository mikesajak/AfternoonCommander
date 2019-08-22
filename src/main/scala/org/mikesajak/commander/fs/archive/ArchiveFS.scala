package org.mikesajak.commander.fs.archive

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

import scala.util.{Failure, Try}

class ArchiveFS(archiveRoot: ArchiveRootDir) extends FS {
  override def id: String = {
    val archiveId = ArchiveStreamFactory.detect(archiveRoot.archiveFile.inStream)
    s"$archiveId-archive:${archiveRoot.name}"
  }

  override def rootDirectory: VDirectory = archiveRoot

  override def attributes: Map[String, String] = Map() // TODO

  override def resolvePath(path: String): Option[VPath] = ??? // TODO

  override def exists(path: VPath): Boolean = archiveRoot.children.contains(path)

  override def delete(path: VPath): Try[Boolean] = Failure(new IllegalStateException("Delete operation is not supported in custom list FS"))

  override def create(path: VPath): Try[Boolean] = Failure(new IllegalStateException("Create operation is not supported in custom list FS"))


  override def freeSpace: Long = ???

  override def totalSpace: Long = ???

  override def usableSpace: Long = ???
}
