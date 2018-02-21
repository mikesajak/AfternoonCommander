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

  def discoverLocalFilesystems(): Seq[LocalFS] = {
    val rootFiles = File.listRoots().toSeq
    rootFiles.map(new LocalFS(_))
  }

  def isLocal(path: VPath): Boolean =
    discoverLocalFilesystems().contains(path.fileSystem)

  private val PathPattern = raw"(\S+)://(.+)".r

  def resolvePath(path: String): Option[VPath] = {
    val (fsId, fsPath) = path match {
        case PathPattern(id, _) => (id, path)
        case _ => ("local", s"local://$path")
      }

      filesystems.find(fs => fs.id == fsId)
        .map(fs => fs.resolvePath(fsPath))
  }

  def homePath: String = {
    //    val fsv = FileSystemView.getFileSystemView
    //    LocalFS.mkLocalPathName(fsv.getHomeDirectory.getAbsolutePath)
    LocalFS.mkLocalPathName(System.getProperty("user.home"))
  }

  def homeDir: VDirectory = resolvePath(homePath).get.directory

}