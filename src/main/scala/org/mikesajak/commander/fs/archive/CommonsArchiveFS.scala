package org.mikesajak.commander.fs.archive

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

class CommonsArchiveFS(archiveRoot: CommonsArchiveRootDir) extends FS {
  override val id: String = {
    val archiveId = ArchiveStreamFactory.detect(archiveRoot.archiveFile.inStream)
    s"$archiveId-archive:${archiveRoot.name}"
  }

  override val rootDirectory: VDirectory = archiveRoot

  override val attributes: Map[String, String] = Map() // TODO

  override def resolvePath(path: String): Option[VPath] = ??? // TODO

  override def freeSpace: Long = rootDirectory.parent.map(_.fileSystem.freeSpace)
                                              .getOrElse(0)

  override def totalSpace: Long = rootDirectory.parent.map(_.fileSystem.totalSpace)
                                               .getOrElse(0)

  override def usableSpace: Long = rootDirectory.parent.map(_.fileSystem.usableSpace)
                                                .getOrElse(0)
}
