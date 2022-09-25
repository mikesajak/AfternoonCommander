package org.mikesajak.commander

import com.google.common.eventbus.Subscribe
import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import org.mikesajak.commander.fs.{FilesystemsManager, VDirectory}
import org.mikesajak.commander.ui.controller.DirViewEvents.CurrentDirChangeNotification
import org.mikesajak.commander.ui.controller.PanelId
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}
import org.mikesajak.commander.util.TextUtil.firstLowerCase
import scribe.Logging

class HistoryCtrl(val name: String, config: Configuration, limit: Int = 15) extends Logging {
  private var dirList = collection.mutable.Queue[VDirectory]()
  private val configKey = s"${ConfigKeys.History}.$name"

  def init(fsMgr: FilesystemsManager): Unit = {
    val savedHistory = config.stringSeqProperty(configKey).value
    logger.info(s"$name - Read saved history: $savedHistory")

    val historyList = savedHistory.map(entries => entries.flatMap(bookmarkPath => fsMgr.resolvePath(bookmarkPath)
                                                                                       .map(_.directory)))
                                  .getOrElse(Seq())
    dirList.enqueueAll(historyList)
  }

  def add(dir: VDirectory): Unit = {
    logger.debug(s"$name - Adding element $dir")
    dirList = dirList.filter(d => d != dir)
    dirList.enqueue(dir)

    while (dirList.size > limit)
      dirList.dequeue()

    config.stringSeqProperty(configKey) := dirList.map(_.toString).toSeq
  }

  def removeLast(): VDirectory = {
    val dir = dirList.head
    logger.debug(s"$name - Removing element $dir")
    dirList = dirList.tail
    config.stringSeqProperty(configKey) := dirList.map(_.toString).toSeq
    dir
  }

  def last: Option[VDirectory] = dirList.lastOption
  def last(num: Int): Option[VDirectory] = {
    val it = dirList.reverseIterator.drop(num)
    it.nextOption()
  }

  def isEmpty: Boolean = dirList.isEmpty
  def nonEmpty: Boolean = dirList.nonEmpty

  def getAll: Seq[VDirectory] = dirList.reverse.toSeq
  def clear(): Unit = {
    dirList.clear()
    config.stringSeqProperty(configKey) := dirList.map(_.toString).toSeq
  }

  override def toString = s"HistoryMgr($name) [$dirList]"
}

class HistoryMgr(config: Configuration) {
  val globalHistoryCtrl = new HistoryCtrl("global", config)

  val panelHistoryCtrl: Map[PanelId, HistoryCtrl] =
    Map[PanelId, HistoryCtrl](LeftPanel -> new HistoryCtrl(firstLowerCase(LeftPanel.toString), config),
                              RightPanel -> new HistoryCtrl(firstLowerCase(RightPanel.toString), config))

  def init(fsMgr: FilesystemsManager): Unit = {
    globalHistoryCtrl.init(fsMgr)
    panelHistoryCtrl.values.foreach(_.init(fsMgr))
  }

  //noinspection UnstableApiUsage
  @Subscribe
  def curDirChanged(event: CurrentDirChangeNotification): Unit = {
    globalHistoryCtrl.add(event.curDir)
    panelHistoryCtrl.foreach { case (panelId, ctrl) =>
      if (event.panelId == panelId)
        ctrl.add(event.curDir)
    }
  }
}
