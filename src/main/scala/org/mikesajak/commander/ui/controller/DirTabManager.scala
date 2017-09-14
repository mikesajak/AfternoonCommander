package org.mikesajak.commander.ui.controller

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.VDirectory

/**
  * Created by mike on 07.05.17.
  */
class DirTabManager(panelId: PanelId) {
  private val logger = Logger(this.getClass)

  private var tabs = IndexedSeq[TabData]()
  private var selectedTabIdx0 = 0

  logger.debug(s"Creating DirTabManager with panelId=$panelId")

  def addTab(dir: VDirectory, controller: DirTableControllerIntf): Unit = {
    tabs :+= TabData(dir, controller)
  }

  def removeTab(idx: Int): Unit = {
    tabs = tabs.patch(idx, Nil, 1)
  }

  def clearTabs(): Unit = {
    tabs = IndexedSeq()
  }

//  def updateTab(idx: Int, dir: VDirectory): Unit = {
//    val curTab = tabs(idx)
//    tabs = tabs.patch(idx, Seq(new TabData(dir, curTab.controller)), 1)
//  }

  def tab(idx: Int): TabData = tabs(idx)

  def selectedTab: TabData = tabs(selectedTabIdx0)

  def selectedTabIdx: Int = selectedTabIdx0
  def selectedTabIdx_=(newIdx: Int): Unit = {
    logger.debug(s"Tab selection change: panelId=$panelId, tab selection: $selectedTabIdx0 -> $newIdx")
    selectedTabIdx0 = newIdx
  }
}

case class TabData(dir: VDirectory, controller: DirTableControllerIntf) {

}
