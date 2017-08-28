package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.fs.VDirectory

/**
  * Created by mike on 07.05.17.
  */
class DirTabManager(panelId: PanelId) {
  private var tabs = IndexedSeq[TabData]()

  println(s"Creating DirTabManager with panelId=$panelId")

  def addTab(dir: VDirectory): Unit = {
    tabs :+= new TabData(dir)
  }

  def removeTab(idx: Int): Unit = {
    tabs = tabs.patch(idx, Nil, 1)
  }

  def clearTabs(): Unit = {
    tabs = IndexedSeq()
  }

  def updateTab(idx: Int, dir: VDirectory): Unit = {
    tabs = tabs.patch(idx, Seq(new TabData(dir)), 1)
  }

  def tab(idx: Int): TabData = tabs(idx)
}

class TabData(val dir: VDirectory) {

}
