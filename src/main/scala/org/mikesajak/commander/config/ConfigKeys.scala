package org.mikesajak.commander.config

object ConfigKeys {
  val ExitConfirmation = "general.exitConfirmation"
  val ShowHiddenFiles = "filePanel.showHidden"
  val TransferBufferSize = "transfer.bufferSize"

  // runtime
  val WindowHeight = "runtime.window.height"
  val WindowWidth = "runtime.window.width"

  val Bookmarks = "runtime.general.bookmarks"
  val History = "runtime.general.history"

  val filePanelCategory = "runtime.filePanel"
  val filePanelColumnCategory = s"$filePanelCategory.column"

  val NameColumnWidth = s"$filePanelColumnCategory.name.width"
  val ExtColumnWidth = s"$filePanelColumnCategory.extension.width"
  val SizeColumnWidth = s"$filePanelColumnCategory.size.width"
  val ModifiedColumnWidth = s"$filePanelColumnCategory.modified.width"
  val AttribsColumnWidth = s"$filePanelColumnCategory.attribs.width"
}
