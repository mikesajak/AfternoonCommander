package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.fs.VPath

trait CursorSelectionAware {
  def setSelectedPath(path: Option[VPath]): Unit
}
