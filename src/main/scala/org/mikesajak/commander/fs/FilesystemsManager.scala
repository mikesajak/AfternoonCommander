package org.mikesajak.commander.fs

import java.io.File

import org.mikesajak.commander.fs.local.LocalFS

/**
*  Created by mike on 25.10.14.
*/
class FilesystemsManager {
  private var filesystems = Seq[FS]()

  def rootFilesystems: Seq[FS] = filesystems

  def registerFilesystem(fs: FS): Unit = filesystems :+= fs

  def init(): Unit = {
    discoverLocalFilesystems()
      .foreach(registerFilesystem)
  }

  def discoverLocalFilesystems(): List[LocalFS] = {
    val rootFiles = File.listRoots().toList
    rootFiles.map(new LocalFS(_))
  }

  private val PathPattern = raw"(\S+)://(.+)".r

  def resolvePath(path: String): Option[VPath] = {
      path match {
        case PathPattern(fsId, fsPath) =>
          filesystems.find(fs => fs.id == fsId)
            .map(fs => fs.resolvePath(path))
        case _ => None
      }
  }
}