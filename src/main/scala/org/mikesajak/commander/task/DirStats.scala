package org.mikesajak.commander.task

import org.mikesajak.commander.fs.{VDirectory, VFile}
import org.mikesajak.commander.units.DataUnit

case class DirStats(numFiles: Int, numDirs: Int, size: Long, depth: Int) {
  def +(other: DirStats): DirStats =
    DirStats(numFiles + other.numFiles, numDirs + other.numDirs, size + other.size, math.max(depth, other.depth))

  override def toString: String =
    s"DirCounts(numFiles=$numFiles, numDirs=$numDirs, size=${DataUnit.formatDataSize(size)}, depth=$depth)}"
}

object DirStats {
  val Empty = DirStats(0,0,0,0)

  def ofDir(dir: VDirectory, level: Int = 0): DirStats = {
    val subDirs = dir.childDirs
    val files = dir.childFiles
    val filesSize = files.map(_.size).sum
    val dirsSize = subDirs.map(_.size).sum

    DirStats(files.size, subDirs.size, filesSize + dirsSize, level)
  }

  def ofDirs(dirs: Seq[VDirectory], level: Int = 0): DirStats =
    dirs.map(DirStats.ofDir(_)).foldLeft(Empty)((acc, dir) => acc + dir)

  def ofFile(file: VFile, level: Int= 0): DirStats =
    DirStats(1, 0, file.size, level)

  def ofFiles(files: Seq[VFile], level: Int = 0): DirStats =
    DirStats(files.size, 0, files.map(_.size).sum, level)
}

class DirStatsProcessor extends PathProcessor[DirStats] {
  override val title = "Count directory statistics"

  override def process(files: Seq[VFile], dirs: Seq[VDirectory], level: Int): DirStats =
    DirStats.ofFiles(files, level) + DirStats.ofDirs(dirs, level)

  override def Empty: DirStats = DirStats.Empty

  override def merge(res1: DirStats, res2: DirStats): DirStats = res1 + res2
}