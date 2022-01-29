package org.mikesajak.commander.fs

import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager
import org.mikesajak.commander.OSResolver
import org.mikesajak.commander.OSType.Windows
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.local.LocalFS
import org.mikesajak.commander.util.PathUtils.{depthToRoot, findParent}
import org.mikesajak.commander.util.Utils
import scribe.Logging

import java.io.File
import java.nio.file.{FileStore, FileSystems}
import javax.swing.filechooser.FileSystemView
import scala.jdk.CollectionConverters._
import scala.util.Try

/**
*  Created by mike on 25.10.14.
*/
class FilesystemsManager(osResolver: OSResolver, config: Configuration) extends Logging {
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
        .filter(fs => !isInternalFs(fs.`type`()))
        .flatMap(fs0 => parseFileStore(fs0))
        .toMap

    val fallbackFss = fallbackFilesystems()
    val usbFilesystems = discoverUsbDrives()

    val filesystems =
      Utils.merge(usbFilesystems, fss, fallbackFss) { (_, attribs1, attribs2) =>
        Utils.merge(attribs1, attribs2) { (_, v1, _) => v1 }
      }

    val fsView = FileSystemView.getFileSystemView
    val fss2 = filesystems
        .filter { case (_, attribs) => attribs.get("type").forall(fs => !isInternalFs(fs)) }
        .filter { case (rootDir, _) => new File(rootDir).exists }
        .map { case (rootDir, attribs) =>
          val rootDirFile = new File(rootDir)
          val attribs2 = List(Option(fsView.getSystemDisplayName(rootDirFile)).map(l => "label" -> l),
                              Option(fsView.getSystemTypeDescription(rootDirFile)).map(i => "info" -> i))
                            .flatten
                            .toMap

          val attribs3 = Utils.merge(attribs, attribs2) { case (_, v1, _) => v1 }
          new LocalFS(rootDirFile, attribs3)
        }
      .toSeq

    fss2
  }

  private val internalFilesystems = prepareInternalFilesystems()

  private final def prepareInternalFilesystems() = {
    // TODO: find better/proper way to filter out unwanted filesystems (TODO2: what about windows??)

    def compilePattern(pattern: String) = {
      val compiledOpt = Try { pattern.r }
      if (compiledOpt.isFailure) logger.info(s"Ignoring invalid internal filesystem pattern: $pattern")
      compiledOpt
    }

    config.stringSeqProperty("internal.internalFilesystems").value
          .getOrElse(List())
          .flatMap(pattern => compilePattern(pattern).toOption)
  }

  private def isInternalFs(fsType: String): Boolean = {
    val internal = internalFilesystems.exists(r => r.findFirstMatchIn(fsType).isDefined)
    internal
  }

  private def parseFileStore(fileStore: FileStore) =
    if (osResolver.getOSType == Windows) parseWindowsFileStore(fileStore)
    else parseUnixFileStore(fileStore)

  private def parseWindowsFileStore(fileStore: FileStore): Option[(String, Map[String, String])] = {
    fileStore.toString match {
      case FileStorePattern(_, drive) =>
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

  def isProperPathPattern(pathName: String): Boolean = pathName match {
    case PathPattern(_, _) => true
    case _ => false
  }

  def resolvePath(path: String, onlyExisting: Boolean = false, forceDir: Boolean = false): Option[VPath] =
    filesystems.view
               .map(fs => fs.resolvePath(path, forceDir)
                            .filter(path => !onlyExisting || path.exists))
               .collectFirst { case Some(x) => x }

  def homePath: String = {
    //    val fsv = FileSystemView.getFileSystemView
    //    LocalFS.mkLocalPathName(fsv.getHomeDirectory.getAbsolutePath)
    LocalFS.mkLocalPathName(System.getProperty("user.home"))
  }

  def findFilesystemFor(path: VPath): FS = {
    val matchingFss = discoverFilesystems().filter(fs => findParent(path, fs.rootDirectory).isDefined)
    //    val fs = matchingFss.foldLeft(dir.fileSystem)((a,b) => if (depthToRoot(a.rootDirectory) > depthToRoot(b.rootDirectory)) a else b)
    matchingFss.maxBy(f => depthToRoot(f.rootDirectory))
  }

  def homeDir: VDirectory = resolvePath(homePath).get.directory

}