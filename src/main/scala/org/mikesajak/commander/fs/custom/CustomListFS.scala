package org.mikesajak.commander.fs.custom

import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

import scala.util.{Failure, Try}

class CustomListFS(val root: CustomListAsDirectory) extends FS {
  override val id: String = "custom-list"

  override def exists(path: VPath): Boolean = root.children.contains(path)

  override def delete(path: VPath): Try[Boolean] = Failure(new IllegalStateException("Delete operation is not supported in custom list FS"))

  override def create(path: VPath): Try[Boolean] = Failure(new IllegalStateException("Create operation is not supported in custom list FS"))

  override def freeSpace: Long = root.parent.get.fileSystem.freeSpace
  override def totalSpace: Long = root.parent.get.fileSystem.totalSpace
  override def usableSpace: Long = root.parent.get.fileSystem.usableSpace

  override def rootDirectory: VDirectory = root

  override def resolvePath(path: String): Option[VPath] = None

  override def toString = s"CustomListFS($id, $rootDirectory, $attributes)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[CustomListFS]

  override def equals(other: Any): Boolean = other match {
    case that: CustomListFS =>
      (that canEqual this) &&
          root == that.root
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(root)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override def attributes: Map[String, String] = root.parent.get.fileSystem.attributes
}
