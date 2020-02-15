package org.mikesajak.commander.ui.controller

import com.google.common.eventbus.Subscribe
import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.EventBus
import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.ui.controller.DirViewEvents.CurrentDirChangeNotification
import org.mikesajak.commander.ui.controller.TabEvents._

/**
  * Created by mike on 07.05.17.
  */
class DirTabManager(panelId: PanelId, eventBus: EventBus) {
  private val logger = Logger(this.getClass)

  private var tabs0 = IndexedSeq[TabData]()
  private var selectedTabIdx0 = 0

  logger.debug(s"Creating DirTabManager with panelId=$panelId")

  def tabs: IndexedSeq[TabData] = tabs0

  def addTab(dir: VDirectory, controller: DirTableController): Unit = {
    tabs0 :+= TabData(dir, controller)

    eventBus.publish(TabAdded(panelId, tabs0.size, dir))
  }

  def removeTab(idx: Int): Unit = {
    val dir = tabs0(idx).dir
    tabs0 = tabs0.patch(idx, Nil, 1)

    eventBus.publish(TabRemoved(panelId, idx, dir))
  }

  def clearTabs(): Unit = {
    tabs0 = IndexedSeq()

    eventBus.publish(TabsCleared(panelId))
  }

//  def updateTab(idx: Int, dir: VDirectory): Unit = {
//    val curTab = tabs0(idx)
//    tabs0 = tabs0.patch(idx, Seq(new TabData(dir, curTab.controller)), 1)
//  }

  def tab(idx: Int): TabData = tabs0(idx)

  def selectedTab: TabData = tabs0(selectedTabIdx0)

  def selectedTabIdx: Int = selectedTabIdx0
  def selectedTabIdx_=(newIdx: Int): Unit = {
    logger.debug(s"Tab selection change: panelId=$panelId, tab selection: $selectedTabIdx0 -> $newIdx")
    selectedTabIdx0 = newIdx

    eventBus.publish(TabSelected(panelId, newIdx))
  }

  //noinspection UnstableApiUsage
  @Subscribe
  def onCurrentDirChange(event: CurrentDirChangeNotification): Unit = {
    if (event.panelId == panelId)
      selectedTab.dir = event.curDir
  }
}

case class TabData(var dir: VDirectory, controller: DirTableController)

object TabEvents {

  case class TabAdded(panelId: PanelId, idx: Int, dir: VDirectory)
  case class TabRemoved(panelId: PanelId, idx: Int, dir: VDirectory)
  case class TabsCleared(panelId: PanelId)

  case class TabSelected(panelId: PanelId, idx: Int)

}