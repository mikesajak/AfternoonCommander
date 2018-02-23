package org.mikesajak.commander.fs

import java.io.File
import java.nio.file.FileSystems

import org.mikesajak.commander.fs.local.LocalFS

import scala.collection.JavaConverters._

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

  // TODO: find proper way to filter out unwanted filesystems (TODO2: what about windows??)
  private val internalFilesystems = List("cgroup.*".r, "systemd.*".r,
    "udev".r, "devpts".r, "proc".r, "(tmp|sys|security|config|debug|hugetlb|squash|auto|ns)fs".r,
    "pstore".r, "mqueue".r)

  def discoverFilesystems() = {
    var fss =FileSystems.getDefault.getFileStores.asScala
      .filter(fs => !internalFilesystems.exists(r => r.findFirstMatchIn(fs.`type`()).isDefined))
      .toSeq
    fss
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