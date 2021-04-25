package org.mikesajak.commander.status

import com.google.common.eventbus.Subscribe
import org.mikesajak.commander.EventBus
import org.mikesajak.commander.fs.VPath
import org.mikesajak.commander.status.StatusChangeEvents.{PanelSelected, PanelSelectionLogger}
import org.mikesajak.commander.ui.controller.PanelId.LeftPanel
import org.mikesajak.commander.ui.controller.{DirTabManager, PanelId}
import scribe.Logging


trait PanelData {
  def directory: VPath
  def cursorAt: VPath
  def selection: Seq[VPath]
}

class StatusMgr(val leftDirTabMgr: DirTabManager, val rightDirTabMgr: DirTabManager,
                eventBus: EventBus) extends Logging {

  private var selectedPanel0: PanelId = LeftPanel

  eventBus.register(new PanelSelectionLogger)

  def selectedPanel: PanelId = selectedPanel0
  def selectedPanel_=(panelId: PanelId): Unit = {
//    if (panelId != selectedPanel0) {
      val oldSelectedPanel = selectedPanel0
      selectedPanel0 = panelId
      eventBus.publish(PanelSelected(oldSelectedPanel, selectedPanel0))
//    }
  }

  def unselectedPanel: PanelId = PanelId.oppositePanel(selectedPanel)

  def tabManager(panelId: PanelId): DirTabManager =
    if (panelId == LeftPanel) leftDirTabMgr else rightDirTabMgr

  def selectedTabManager: DirTabManager = tabManager(selectedPanel)
  def unselectedTabManager: DirTabManager = tabManager(unselectedPanel)
}


object StatusChangeEvents {
  case class PanelSelected(oldPanelId: PanelId, newPanelId: PanelId)

  class PanelSelectionLogger extends Logging {
    @Subscribe
    def handle(event: PanelSelected): Unit =
      logger.debug(s"Change panel selection: panelId: ${event.oldPanelId} -> ${event.newPanelId}")
  }
}