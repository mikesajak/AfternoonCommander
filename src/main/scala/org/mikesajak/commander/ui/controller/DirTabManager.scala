package org.mikesajak.commander.ui.controller

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.VDirectory

/**
  * Created by mike on 07.05.17.
  */
class DirTabManager(panelId: PanelId) {
  private val logger = Logger(this.getClass)

  private var tabs0 = IndexedSeq[TabData]()
  private var selectedTabIdx0 = 0

  logger.debug(s"Creating DirTabManager with panelId=$panelId")

  def tabs: IndexedSeq[TabData] = tabs0

  def addTab(dir: VDirectory, controller: DirTableControllerIntf): Unit = {
    tabs0 :+= TabData(dir, controller)
  }

  def removeTab(idx: Int): Unit = {
    tabs0 = tabs0.patch(idx, Nil, 1)
  }

  def clearTabs(): Unit = {
    tabs0 = IndexedSeq()
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
  }
}

case class TabData(var dir: VDirectory, controller: DirTableControllerIntf)
