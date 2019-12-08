package org.mikesajak.commander

import com.google.common.eventbus.Subscribe
import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.ui.controller.DirViewEvents.CurrentDirChange
import org.mikesajak.commander.ui.controller.PanelId
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}

class HistoryCtrl(val name: String, limit: Int = 15) {
  private val logger = Logger[HistoryCtrl]
  private var dirList = collection.mutable.Queue[VDirectory]()

  def add(dir: VDirectory): Unit = {
    logger.debug(s"$name - Adding element $dir")
    dirList = dirList.filter(d => d != dir)
    dirList.enqueue(dir)

    while (dirList.size > limit)
      dirList.dequeue
  }

  def removeLast(): VDirectory = {
    val dir = dirList.head
    logger.debug(s"$name - Removing element $dir")
    dirList = dirList.tail
    dir
  }

  def last: Option[VDirectory] = dirList.lastOption

  def isEmpty: Boolean = dirList.isEmpty
  def nonEmpty: Boolean = dirList.nonEmpty

  def getAll: Seq[VDirectory] = dirList.reverse
  def clear(): Unit = dirList.clear()

  override def toString = s"HistoryMgr($name) [$dirList]"
}

class HistoryMgr {
  val globalHistoryCtrl = new HistoryCtrl("Global")

  val panelHistoryCtrl: Map[PanelId, HistoryCtrl] =
    Map[PanelId, HistoryCtrl](LeftPanel -> new HistoryCtrl(LeftPanel.toString),
                              RightPanel -> new HistoryCtrl(RightPanel.toString))

  //noinspection UnstableApiUsage
  @Subscribe
  def curDirChanged(event: CurrentDirChange): Unit = {
    event.prevDir.foreach { dir =>
      globalHistoryCtrl.add(dir)
      panelHistoryCtrl.foreach { case (panelId, ctrl) =>
        if (event.panelId == panelId)
          ctrl.add(dir)
      }
    }
  }
}
