package org.mikesajak.commander.task

import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}

case class IOTaskSummary(numDirs: Int, numFiles: Int, totalSize: Long,
                         errors: List[(VPath, String)]) {

  def hasErrors: Boolean = errors.nonEmpty
  def isSuccessful: Boolean = errors.isEmpty
  def isFailed: Boolean = hasErrors

  def merge(that: IOTaskSummary): IOTaskSummary =
    IOTaskSummary(numDirs + that.numDirs,
                  numFiles + that.numFiles,
                  totalSize + that.totalSize,
                  errors ::: that.errors)
}

object IOTaskSummary {
  def success(path: VPath): IOTaskSummary = path match {
    case d: VDirectory => IOTaskSummary(1, 0, 0, List())
    case f: VFile => IOTaskSummary(0, 1, f.size, List())
  }

  def failed(path: VPath, errorMsg: String) = IOTaskSummary(0, 0, 0, List((path, errorMsg)))

  def empty = IOTaskSummary(0, 0, 0, List())
}