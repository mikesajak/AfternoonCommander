package org.mikesajak.commander

import org.mikesajak.commander.fs.VDirectory

import scala.collection.immutable.Queue

class HistoryMgr(limit: Int = 15) {
  private var dirList = Queue[VDirectory]()

  def add(dir: VDirectory): Unit = {
    dirList = dirList
        .filter(d => d != dir)
        .enqueue(dir)

    while (dirList.size > limit)
      dirList.dequeue
  }
  def removeLast(): VDirectory = {
    val dir = dirList.head
    dirList = dirList.tail
    dir
  }

  def getAll: Seq[VDirectory] = dirList
  def clear(): Unit = dirList = Queue()
}
