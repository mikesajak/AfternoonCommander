package org.mikesajak.commander.fs

import java.io.File
import java.nio.file.FileSystems

import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager
import org.mikesajak.commander.fs.local.LocalFS

import scala.collection.JavaConverters._

/**
*  Created by mike on 25.10.14.
*/
class FilesystemsManager {
  private var filesystems = Seq[FS]()

  private val driveDetector = new USBDeviceDetectorManager()

  def rootFilesystems: Seq[FS] = filesystems

  def registerFilesystem(fs: FS): Unit = filesystems :+= fs

  def init(): Unit = {
    discoverFilesystems()
      .foreach(registerFilesystem)
  }

  // TODO: find proper way to filter out unwanted filesystems (TODO2: what about windows??)
  private val internalFilesystems = List("cgroup.*".r, "systemd.*".r,
    "udev".r, "devpts".r, "proc".r, "(tmp|sys|security|config|debug|hugetlb|squash|auto|ns)fs".r,
    "pstore".r, "mqueue".r)

  private val FileStorePattern = raw"(?:(.+) )?\((.+)\)".r

  def discoverFilesystems(): Seq[FS] = {

    val usbDevices = driveDetector.getRemovableDevices.asScala
      .map(dev => (dev.getRootDirectory.getAbsolutePath, dev)).toMap

    val fss =
    FileSystems.getDefault.getFileStores.asScala
      .filter(fs => !internalFilesystems.exists(r => r.findFirstMatchIn(fs.`type`()).isDefined))
      .flatMap{ fs0 =>
        fs0.toString match {
          case FileStorePattern(mountPoint, drive) =>
            val rootDir = new File(mountPoint)
            val usbDev = usbDevices.get(rootDir.getAbsolutePath)
            val attributes = List(Some("type" -> fs0.`type`()),
                                  Option(drive).map(d => "drive" -> drive),
                                  usbDev.map(d => "label" -> d.getSystemDisplayName),
                                  usbDev.map(d => "usb" -> "true"),
                                  usbDev.map(d => "removable" -> "true"))
              .flatten
              .toMap
            Some(new LocalFS(rootDir, attributes))
          case _ => None
        }
      }
      .toSeq
    fss
  }

  def isLocal(path: VPath): Boolean =
    discoverFilesystems().contains(path.fileSystem)

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