package org.mikesajak.commander.fs

import java.io.{InputStream, OutputStream}
import java.time.Instant

import org.mikesajak.commander.util.PathUtils

trait VPath {
  def name: String
  def parent: Option[VDirectory]
  def directory: VDirectory
  def absolutePath: String
  def segments: Seq[String] = PathUtils.collectParents(this)
  def modificationDate: Instant
  def attribs: String
  def isDirectory: Boolean
  def isFile: Boolean = !isDirectory
  def fileSystem: FS
  def size: Long
}

trait VFile extends VPath {
  def extension: Option[String] = {
    val extPos = name.lastIndexOf('.')
    if (extPos != -1 && extPos < name.length - 1) {
      Some(name.slice(extPos+1, name.length))
    } else None
  }

  def inStream: InputStream
  def outStream: OutputStream
}

trait VDirectory extends VPath {
  def children: Seq[VPath]
  def childDirs: Seq[VDirectory]
  def childFiles: Seq[VFile]

  def mkChildDir(child: String): VDirectory
  def mkChildFile(child: String): VFile

  override val isDirectory = true
  override val directory: VDirectory = this
}



