package org.mikesajak.commander.status

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.ui.controller.PanelId.LeftPanel
import org.mikesajak.commander.ui.controller.{DirTabManager, PanelId}


trait PanelData {
  def directory: VPath
  def cursorAt: VPath
  def selection: Seq[VPath]
}

class StatusMgr(val leftDirTabMgr: DirTabManager, val rightDirTabMgr: DirTabManager) {
  private val logger = Logger(getClass)

  private var selectedPanel0: PanelId = LeftPanel
  private var selectedPanelListeners = List[PanelSelectionListener]()

  def selectedPanel: PanelId = selectedPanel0
  def selectedPanel_=(panelId: PanelId): Unit = {
//    if (panelId != selectedPanel0) {
      val oldSelectedPanel = selectedPanel0
      selectedPanel0 = panelId
      logger.debug(s"Change panel selection: panelId: $oldSelectedPanel -> $selectedPanel0")
      selectedPanelListeners.foreach(_.apply(oldSelectedPanel, selectedPanel0))
//    }
  }
  def addPanelSelectionListener(listener: PanelSelectionListener): Unit =
    selectedPanelListeners ::= listener
  def removePanelSelectionListener(listener: PanelSelectionListener): Unit =
    selectedPanelListeners = selectedPanelListeners.filter(l => l != listener)


  def tabManager(panelId: PanelId): DirTabManager =
    if (panelId == LeftPanel) leftDirTabMgr else rightDirTabMgr

  def selectedTabManager: DirTabManager = tabManager(selectedPanel)

  trait PanelSelectionListener extends Function2[PanelId, PanelId, Unit] {
    def apply(oldPanelId: PanelId, newPanelId: PanelId): Unit
  }
}
