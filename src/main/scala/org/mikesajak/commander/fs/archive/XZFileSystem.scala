package org.mikesajak.commander.fs.archive

import org.mikesajak.commander.fs.{FS, VPath}

class XZFileSystem(override val rootDirectory: XZRootDir) extends FS {
  override def id: String = s"XZ-archive:${rootDirectory.name}"

  override def attributes: Map[String, String] = Map()

  override def resolvePath(path: String, forceDir: Boolean): Option[VPath] = ???

  override def freeSpace: Long = rootDirectory.parent.map(_.fileSystem.freeSpace)
                                              .getOrElse(0)

  override def totalSpace: Long = rootDirectory.parent.map(_.fileSystem.totalSpace)
                                               .getOrElse(0)

  override def usableSpace: Long = rootDirectory.parent.map(_.fileSystem.usableSpace)
                                                .getOrElse(0)
}
