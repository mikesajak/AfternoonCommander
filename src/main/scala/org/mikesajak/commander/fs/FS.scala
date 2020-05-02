package org.mikesajak.commander.fs

trait FS {
  def id: String
  def rootDirectory: VDirectory
  def attributes: Map[String, String]

  def resolvePath(path: String, forceDir: Boolean = false): Option[VPath]

  def freeSpace: Long
  def totalSpace: Long
  def usableSpace: Long
}

object FS {
  def rootDirOf(path: VPath): VDirectory = {
    var curDir = path.directory
    var parent= curDir.parent

    while (parent.isDefined) {
      curDir = parent.get
      parent= curDir.parent
    }
    curDir
  }
}
