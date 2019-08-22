package org.mikesajak.commander.fs.archive

import java.time.Instant

import org.apache.commons.compress.archivers.ArchiveEntry
import org.mikesajak.commander.fs._
import org.mikesajak.commander.util.PathUtils

trait ArchiveDirectory extends VDirectory {
  val archiveNode: ArchiveNode
  val childNodes: Seq[ArchiveNode]

  override val directory: ArchiveDirectory = this
  override val size = 0

  override def mkChildDir(child: String): VDirectory = throw new IllegalStateException("Creating child directory not supported")
  override def mkChildFile(child: String): VFile = throw new IllegalStateException("Creating child file not supported")

  override lazy val childFiles: Seq[VFile] =
    childNodes.filter(node => archiveNode.isDirectSubNode(node))
              .filterNot { case ArchiveNode(_, Some(archiveEntry)) => archiveEntry.isDirectory }
              .map(node => new ArchiveFile(node.archiveEntry.get, this))

  override lazy val childDirs: Seq[VDirectory] = {
    val subDirsMap = childNodes
        .filter(node => archiveNode.isIndirectSubNode(node))
        .groupBy(node => archiveNode.getDirectSubNodePath(node))

    val createdIndirectSubDirs = subDirsMap.map { case (pathToRoot, childNodes) =>
      new ArchiveSubDir(this, ArchiveNode(pathToRoot), childNodes)
    }

    val leafDirs = childNodes.filterNot(node => subDirsMap.contains(node.pathToRoot))
                             .filter { case ArchiveNode(_, Some(archiveEntry)) => archiveEntry.isDirectory }
                             .map(node => new ArchiveSubDir(this, node, Seq()))

    leafDirs ++ createdIndirectSubDirs
  }
}

class ArchiveRootDir(val archiveFile: VFile, archiveEntries: Seq[ArchiveEntry])
    extends ArchiveDirectory {
  override val archiveNode: ArchiveNode = ArchiveNode(IndexedSeq())
  override val childNodes: Seq[ArchiveNode] = archiveEntries.map { entry =>
    val segments = PathUtils.pathSegments(entry.getName)
    ArchiveNode(segments, entry)
  }

  override val name: String = archiveFile.name
  override val parent: Option[VDirectory] = Some(archiveFile.directory)
  override val absolutePath: String = archiveFile.absolutePath
  override val modificationDate: Instant = archiveFile.modificationDate
  override val attributes: Attribs = archiveFile.attributes
  override lazy val fileSystem: FS = new ArchiveFS(this)
  override val size: Long = 0

  def isParent(path: VPath): Boolean =
    if (childFiles.contains(path)) true
    else childDirs.collectFirst { case d => d.isParent(path) }
                  .getOrElse(false)

  override def mkChildDir(child: String): VDirectory = throw new IllegalStateException("Creating child directory not supported")
  override def mkChildFile(child: String): VFile = throw new IllegalStateException("Creating child file not supported")


  override def toString = s"ArchiveRootDir(${archiveFile.name})"
}

class ArchiveSubDir(parentDir: VDirectory,
                    override val archiveNode: ArchiveNode,
                    override val childNodes: Seq[ArchiveNode])
    extends ArchiveDirectory {
  override val name: String = archiveNode.name
  override val parent: Option[VDirectory] = Some(parentDir)
  override def absolutePath: String = parent.get.absolutePath
  override def modificationDate: Instant = parent.get.modificationDate
  override val attributes: Attribs = new Attribs(Attrib.Directory, Attrib.Readable)
  override val size: Long = 0

  def isParent(path: VPath): Boolean =
    if (childFiles.contains(path)) true
    else childDirs.collectFirst { case d => d.isParent(path) }
                  .getOrElse(false)

  override val fileSystem: FS = parentDir.fileSystem

  override def mkChildDir(child: String): VDirectory = throw new IllegalStateException("Creating child directory not supported")
  override def mkChildFile(child: String): VFile = throw new IllegalStateException("Creating child file not supported")

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
