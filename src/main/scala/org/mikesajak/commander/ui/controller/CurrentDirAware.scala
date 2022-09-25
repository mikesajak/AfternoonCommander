package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.fs.VDirectory

trait CurrentDirAware {
  def setDirectory(directory: VDirectory): Unit
}
