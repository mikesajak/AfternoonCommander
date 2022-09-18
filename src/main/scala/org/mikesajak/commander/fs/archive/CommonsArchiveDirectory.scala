package org.mikesajak.commander.fs.archive

import org.apache.commons.compress.archivers.ArchiveEntry
import org.mikesajak.commander.archive.ArchiveType
import org.mikesajak.commander.fs._
import org.mikesajak.commander.util.PathUtils

import java.time.Instant
import scala.collection.compat.immutable.ArraySeq

trait CommonsArchiveDirectory extends VDirectory {
  val archiveFile: VFile
  val archiveNode: ArchiveNode
  val childNodes: Seq[ArchiveNode]

  override val directory: CommonsArchiveDirectory = this
  override val size = 0

  private lazy val archiveStreamProvider = new ArchiveStreamProvider(archiveFile)

  override lazy val childFiles: Seq[VFile] =
    childNodes.filter(node => archiveNode.isDirectSubNode(node))
              .filterNot { case ArchiveNode(_, Some(archiveEntry)) => archiveEntry.isDirectory }
              .map(node => new CommonsArchiveFile(archiveStreamProvider, node.archiveEntry.get, this))

  override lazy val childDirs: Seq[VDirectory] = {
    val subDirsMap = childNodes
        .filter(node => archiveNode.isIndirectSubNode(node))
        .groupBy(node => archiveNode.getDirectSubNodePath(node))

    val createdIndirectSubDirs = subDirsMap.map { case (pathToRoot, childNodes) =>
      new CommonsArchiveSubDir(archiveFile, this, ArchiveNode(pathToRoot), childNodes)
    }

    val leafDirs = childNodes.filterNot(node => subDirsMap.contains(node.pathToRoot))
                             .filter(node => archiveNode.isDirectSubNode(node))
                             .filter { case ArchiveNode(_, Some(archiveEntry)) => archiveEntry.isDirectory }
                             .map(node => new CommonsArchiveSubDir(archiveFile,this, node, Seq()))

    leafDirs ++ createdIndirectSubDirs
  }
}

class CommonsArchiveRootDir(override val archiveFile: VFile, archiveType: ArchiveType, archiveEntries: Seq[ArchiveEntry])
    extends CommonsArchiveDirectory {
  override val archiveNode: ArchiveNode = ArchiveNode(IndexedSeq())
  override val childNodes: Seq[ArchiveNode] = archiveEntries.map { entry =>
    val segments = PathUtils.pathSegments(entry.getName)
    ArchiveNode(ArraySeq.unsafeWrapArray(segments), entry)
  }

  override val name: String = archiveFile.name
  override val parent: Option[VDirectory] = Some(archiveFile.directory)
  override val absolutePath: String = s"${archiveFile.absolutePath}/${archiveType.extension}/"
  override val modificationDate: Instant = archiveFile.modificationDate
  override val creationDate: Instant = archiveFile.creationDate
  override val accessDate: Instant = archiveFile.accessDate
  override val attributes: Attribs = archiveFile.attributes
  override lazy val fileSystem: FS = new CommonsArchiveFS(this)
  override val size: Long = 0
  override val permissions: AccessPermissions = archiveFile.permissions

  def isParent(path: VPath): Boolean =
    if (childFiles.contains(path)) true
    else childDirs.collectFirst { case d => d.isParent(path) }
                  .getOrElse(false)

  override val updater: Option[VDirectoryUpdater] = None

  override val exists: Boolean = true

  override val toString = s"ArchiveRootDir(${archiveFile.name})"
}

class CommonsArchiveSubDir(override val archiveFile: VFile,
                           parentDir: VDirectory,
                           override val archiveNode: ArchiveNode,
                           override val childNodes: Seq[ArchiveNode])
    extends CommonsArchiveDirectory {
  override val name: String = archiveNode.name
  override val parent: Option[VDirectory] = Some(parentDir)
  override def absolutePath: String = s"${parentDir.absolutePath}/$name"
  override def modificationDate: Instant = parent.get.modificationDate
  override def creationDate: Instant = parent.get.creationDate
  override def accessDate: Instant = parent.get.accessDate
  override val attributes: Attribs = new Attribs(Attrib.Directory, Attrib.Readable)
  override val size: Long = 0

  def isParent(path: VPath): Boolean =
    if (childFiles.contains(path)) true
    else childDirs.collectFirst { case d => d.isParent(path) }
                  .getOrElse(false)

  override val fileSystem: FS = parentDir.fileSystem

  override val updater: Option[VDirectoryUpdater] = None

  override val exists: Boolean = true

  override def permissions: AccessPermissions = archiveFile.permissions

  override def toString = s"ArchiveSubDir($name)"
}

private[archive] case class ArchiveNode(pathToRoot: IndexedSeq[String], archiveEntry: Option[ArchiveEntry]) {
  val name: String = pathToRoot.lastOption.getOrElse("")

  def isDirectSubNode(subNode: ArchiveNode): Boolean =
    isSubNode(subNode) && pathToRoot.size + 1 == subNode.pathToRoot.size

  def isIndirectSubNode(subNode: ArchiveNode): Boolean =
    isSubNode(subNode) && subNode.pathToRoot.size - pathToRoot.size > 1

  private def isSubNode(subNode: ArchiveNode): Boolean =
    pathToRoot.size < subNode.pathToRoot.size && subNode.pathToRoot.startsWith(pathToRoot)

  def getDirectSubNodePath(subNode: ArchiveNode): IndexedSeq[String] =
    pathToRoot :+ subNode.pathToRoot(pathToRoot.size)
}

private[archive] object ArchiveNode {
  def apply(pathToRoot: IndexedSeq[String]): ArchiveNode = ArchiveNode(pathToRoot, None)
  def apply(pathToRoot: IndexedSeq[String], archiveEntry: ArchiveEntry): ArchiveNode = ArchiveNode(pathToRoot, Some(archiveEntry))
}
