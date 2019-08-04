package org.mikesajak.commander

import com.google.common.eventbus.Subscribe
import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.ui.controller.DirViewEvents.CurrentDirChange
import org.mikesajak.commander.ui.controller.PanelId

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

  def last: Option[VDirectory] = if (dirList.nonEmpty) Some(dirList.front) else None

  def isEmpty: Boolean = dirList.isEmpty
  def nonEmpty: Boolean = dirList.nonEmpty

  def getAll: Seq[VDirectory] = dirList.reverse
  def clear(): Unit = dirList.clear()
}

class HistoryUpdater(historyMgr: HistoryMgr) {
  @Subscribe
  def curDirChanged(event: CurrentDirChange): Unit = {
    event.prevDir.foreach(dir => historyMgr.add(dir))
  }
}

class PanelHistoryUpdater(panelId: PanelId, historyMgr: HistoryMgr) {
  @Subscribe
  def curDirChanged(event: CurrentDirChange): Unit = {
    if (panelId == event.panelId)
      event.prevDir.foreach(dir => historyMgr.add(dir))
  }
}
