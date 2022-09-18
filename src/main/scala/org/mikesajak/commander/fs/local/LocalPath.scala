package org.mikesajak.commander.fs.local

import org.mikesajak.commander.fs.Attrib._
import org.mikesajak.commander.fs._

import java.io.File
import java.nio.file.attribute._
import java.nio.file.{Files, Path, Paths}
import java.time.Instant

trait LocalPath extends VPath {

  val file: File
  val fileSystem: LocalFS

  override def name: String = {
    if (file.getName.isEmpty) file.getAbsolutePath
    else file.getName
  }

  override def absolutePath: String = file.getAbsolutePath

  override def size: Long = file.length()

  override def attributes: Attribs = {
    val b = Attribs.builder()

    val path = file.toPath
    if (Files.isDirectory(path)) b.addAttrib(Attrib.Directory)
    if (Files.isReadable(path)) b.addAttrib(Readable)
    if (Files.isWritable(path)) b.addAttrib(Writable)
    if (Files.isExecutable(path)) b.addAttrib(Executable)
    if (Files.isHidden(path)) b.addAttrib(Hidden)
    if (Files.isSymbolicLink(path)) b.addAttrib(Symlink)

    b.build()
  }

  override def modificationDate: Instant = {
    val attr = Files.readAttributes(file.toPath, classOf[BasicFileAttributes])
    attr.lastModifiedTime().toInstant
  }

  override def creationDate: Instant = {
    val attr = Files.readAttributes(file.toPath, classOf[BasicFileAttributes])
    attr.creationTime().toInstant
  }

  override def accessDate: Instant = {
    val attr = Files.readAttributes(file.toPath, classOf[BasicFileAttributes])
    attr.lastAccessTime().toInstant
  }

  override def parent: Option[VDirectory] = {
    if (file.getParent != null) Some(new LocalDirectory(file.getParentFile, fileSystem))
    else None
  }

  override def directory: VDirectory = {
    if (isDirectory) this.asInstanceOf[VDirectory]
    else parent.get
  }

  override def permissions: AccessPermissions = {
    val fsPath = Paths.get(absolutePath)

    getAclFilePermissions(fsPath)
      .orElse(getUnixFilePermissions(fsPath))
      .getOrElse(getBasicFilePermissions(fsPath))
  }

  private def getUnixFilePermissions(fsPath: Path): Option[UnixAccessPermissions] =
    Option(Files.getFileAttributeView(fsPath, classOf[PosixFileAttributeView]))
      .map(posixFileAttributeView => AccessPermissions.apply(posixFileAttributeView))

  private def getAclFilePermissions(fsPath: Path): Option[AccessPermissions] =
    Option(Files.getFileAttributeView(fsPath, classOf[AclFileAttributeView]))
      .map(aclFileAttributeView1 => AccessPermissions.apply(aclFileAttributeView1))

  private def getBasicFilePermissions(fsPath: Path): AccessPermissions = {
    Option(Files.getFileAttributeView(fsPath, classOf[FileOwnerAttributeView]))
      .map(fileOwnerAttributeView => new AccessPermissions(fileOwnerAttributeView.getOwner.getName))
      .getOrElse(new AccessPermissions("n/a"))
  }


  override def toString: String = s"${LocalFS.id}://$absolutePath"

  def canEqual(other: Any): Boolean //= other.isInstanceOf[LocalPath]

  override def equals(other: Any): Boolean = other match {
    case that: LocalPath =>
      (that canEqual this) &&
        file == that.file
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(file)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
