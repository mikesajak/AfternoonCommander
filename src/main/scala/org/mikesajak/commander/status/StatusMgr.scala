package org.mikesajak.commander.status

import org.mikesajak.commander.fs.VPath

sealed trait PanelSelection

object PanelSelection {
  object Left extends PanelSelection
  object Right extends PanelSelection
}

trait PanelData {
  def directory: VPath
  def cursorAt: VPath
  def selection: Seq[VPath]
}


object StatusMgr {
  private var selectedPanel = PanelSelection.Left
  private var leftSelectedTab = 0
  private var rightSelectedTab = 0


}
