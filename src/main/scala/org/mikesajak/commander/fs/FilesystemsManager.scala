package org.mikesajak.commander.fs

import java.io.File
import java.nio.file.{FileStore, FileSystems}

import javax.swing.filechooser.FileSystemView
import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager
import org.mikesajak.commander.OSResolver
import org.mikesajak.commander.OSType.Windows
import org.mikesajak.commander.fs.local.LocalFS
import org.mikesajak.commander.util.Utils

import scala.collection.JavaConverters._

/**
*  Created by mike on 25.10.14.
*/
class FilesystemsManager(osResolver: OSResolver) {
  private lazy val driveDetector = new USBDeviceDetectorManager()

  private var filesystems = Seq[FS]()

  def rootFilesystems: Seq[FS] = filesystems

  def registerFilesystem(fs: FS): Unit = filesystems :+= fs

  def init(): Unit = {
    discoverFilesystems()
      .foreach(registerFilesystem)
  }

  private val FileStorePattern = raw"(.+) \((.+)\)".r

  def discoverFilesystems(): Seq[FS] = {
    val fss =
      FileSystems.getDefault.getFileStores.asScala
        .filter(fs => !internalFilesystems.exists(r => r.findFirstMatchIn(fs.`type`()).isDefined))
        .flatMap(fs0 => parseFileStore(fs0))
        .toMap

    val fallbackFss = fallbackFilesystems()
    val usbFilesystems = discoverUsbDrives()

    val filesystems =
      Utils.merge(usbFilesystems, fss, fallbackFss) { (rootDir, attribs1, attribs2) =>
        Utils.merge(attribs1, attribs1) { (k, v1, v2) => v1 }
      }

    val fsView = FileSystemView.getFileSystemView
    val fss2 = filesystems
        .filter { case (rootDir, attribs) => attribs.get("type").forall(isInternalFs) }
        .filter { case (rootDir, attribs) => new File(rootDir).exists }
        .map { case (rootDir, attribs) =>
          val rootDirFile = new File(rootDir)
          val attribs2 = List(Option(fsView.getSystemDisplayName(rootDirFile)).map(l => "label" -> l),
                              Option(fsView.getSystemTypeDescription(rootDirFile)).map(i => "info" -> i))
                            .flatten
                            .toMap

          val attribs3 = Utils.merge(attribs, attribs2) { case (k, v1, v2) => v1 }
          new LocalFS(rootDirFile, attribs3)
        }
      .toSeq

    fss2
  }

  // TODO: find better/proper way to filter out unwanted filesystems (TODO2: what about windows??)
  private val internalFilesystems = List("cgroup.*", "systemd.*",
    "udev", "devpts", "proc", "(tmp|sys|security|config|debug|hugetlb|squash|auto|ns|gv)fs",
    "pstore", "mqueue", "fusectl").map(_.r)

  private def isInternalFs(fsType: String): Boolean =
    !internalFilesystems.exists(r => r.findFirstMatchIn(fsType).isDefined)

  private def parseFileStore(fileStore: FileStore) =
    if (osResolver.getOSType == Windows) parseWindowsFileStore(fileStore)
    else parseUnixFileStore(fileStore)

  private def parseWindowsFileStore(fileStore: FileStore): Option[(String, Map[String, String])] = {
    fileStore.toString match {
      case FileStorePattern(name, drive) =>
        val rootDir = sanitizeDir(drive)
        val attributes = Map("type" -> fileStore.`type`(),
                             "drive" -> drive)
          Some(rootDir -> attributes)
      case _ => None
    }
  }

  private def parseUnixFileStore(fileStore: FileStore): Option[(String, Map[String, String])] = {
    fileStore.toString match {
      case FileStorePattern(root, drive) =>
        val rootDir = sanitizeDir(root)
        val attributes = Map("type" -> fileStore.`type`(),
                             "drive" -> drive)
        Some(rootDir -> attributes)
      case _ => None
    }
  }

  private def sanitizeDir(dir: String) = {
    val d = if (!dir.endsWith(File.separator)) s"$dir${File.separator}" else dir
    new File(d).getAbsolutePath
  }

  private def fallbackFilesystems(): Map[String, Map[String, String]] = {
    File.listRoots().map { r =>
      r.getAbsolutePath -> Map[String, String]()
    }.toMap
  }

  private def discoverUsbDrives(): Map[String, Map[String, String]] = {
    driveDetector.getRemovableDevices.asScala.map { dev =>
      val rootDir = dev.getRootDirectory.getAbsolutePath
      val attributes =
        Map("label" -> dev.getSystemDisplayName,
            "usb" -> "true",
            "removable" -> "true")
      rootDir -> attributes
    }.toMap
  }

  def isLocal(path: VPath): Boolean =
    discoverFilesystems().contains(path.fileSystem)

  private val PathPattern = raw"(\S+)://(.+)".r

  def resolvePath(path: String): Option[VPath] =
    filesystems.toIterator
               .map(fs => fs.resolvePath(path)
                            .filter(p => fs.exists(p)))
               .collectFirst { case Some(x) => x }

  def homePath: String = {
    //    val fsv = FileSystemView.getFileSystemView
    //    LocalFS.mkLocalPathName(fsv.getHomeDirectory.getAbsolutePath)
    LocalFS.mkLocalPathName(System.getProperty("user.home"))
  }

  def homeDir: VDirectory = resolvePath(homePath).get.directory

}