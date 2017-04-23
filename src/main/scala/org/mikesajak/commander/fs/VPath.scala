package org.mikesajak.commander.fs

import java.io.{InputStream, OutputStream}
import java.util.Date

trait VPath {
  def name: String
  def parent: Option[VDirectory]
  def directory: VDirectory
  def absolutePath: String
  def modificationDate: Date
  def attribs: String
  def isDirectory: Boolean
  def isFile: Boolean = !isDirectory
  def fileSystem: FS
}

trait VFile extends VPath {
  def size: Long
  def extension: Option[String]

  def getInStream: InputStream
  def getOutStream: OutputStream
}

trait VDirectory extends VPath {
  def children: Seq[VPath]

  override val isDirectory = true
  override val directory: VDirectory = this
}



