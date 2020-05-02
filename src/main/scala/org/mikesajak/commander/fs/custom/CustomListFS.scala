package org.mikesajak.commander.fs.custom

import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

class CustomListFS(val root: CustomListAsDirectory) extends FS {
  override val id: String = "custom-list"

  override def freeSpace: Long = root.parent.get.fileSystem.freeSpace
  override def totalSpace: Long = root.parent.get.fileSystem.totalSpace
  override def usableSpace: Long = root.parent.get.fileSystem.usableSpace

  override def rootDirectory: VDirectory = root

  override def resolvePath(path: String, forceDir: Boolean): Option[VPath] = None

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
