package org.mikesajak.commander

import org.mikesajak.commander.fs.VDirectory

class HistoryMgr(limit: Int = 15) {
  private var dirList = collection.mutable.Queue[VDirectory]()

  def add(dir: VDirectory): Unit = {
    dirList = dirList.filter(d => d != dir)
    dirList.enqueue(dir)

    while (dirList.size > limit)
      dirList.dequeue
  }

  def removeLast(): VDirectory = {
    val dir = dirList.head
    dirList = dirList.tail
    dir
  }

  def getAll: Seq[VDirectory] = dirList.reverse
  def clear(): Unit = dirList.clear()
}
