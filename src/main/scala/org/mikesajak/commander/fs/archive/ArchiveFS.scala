package org.mikesajak.commander.fs.archive

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

class ArchiveFS(archiveRoot: ArchiveRootDir) extends FS {
  override def id: String = {
    val archiveId = ArchiveStreamFactory.detect(archiveRoot.archiveFile.inStream)
    s"$archiveId-archive:${archiveRoot.name}"
  }

  override def rootDirectory: VDirectory = archiveRoot

  override def attributes: Map[String, String] = Map() // TODO

  override def resolvePath(path: String): Option[VPath] = ??? // TODO

  override def freeSpace: Long = 0

  override def totalSpace: Long = 0

  override def usableSpace: Long = 0
}
