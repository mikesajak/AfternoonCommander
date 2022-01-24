package org.mikesajak.commander.task

import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.VDirectory
import scribe.Logging

case class DirData(fileSize: Long, var children: Seq[DirData]) {
  def size: Long =
    (children foldLeft fileSize)((accSize, dirData) => accSize + dirData.size)
}

class DirTreeCollectorTask(path: VDirectory) extends jfxc.Task[DirData] with Logging {
  updateTitle(s"DirTreeCollector for: $path")

  override def call(): DirData = try {
    updateProgress(-1, -1)

    processDir(path, 0)
  }

  private def processDir(dir: VDirectory, level: Int): DirData = {
    if (isCancelled)
      throw CancelledException(dir)

    updateMessage(dir.absolutePath)

    val filesSize = dir.childFiles.map(_.size).sum
    val childDirdata = dir.childDirs.map(d => processDir(d, level + 1))

    val dirData = DirData(filesSize, childDirdata)

    updateValue(dirData)

    dirData
  }
}
