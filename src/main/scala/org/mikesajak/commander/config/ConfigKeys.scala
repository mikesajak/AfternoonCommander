package org.mikesajak.commander.config

object ConfigKeys {
  val generalCategory = "general"
  val ExitConfirmation = s"$generalCategory.exitConfirmation"

  val filePanelCategory = "filePanel"
  val ShowHiddenFiles = s"$filePanelCategory.showHidden"

  val PanelTextColor = s"$filePanelCategory.textColor"
  val PanelBgColor1 = s"$filePanelCategory.bgColor1"
  val PanelBgColor2 = s"$filePanelCategory.bgColor2"
  val PanelSelectionColor = s"$filePanelCategory.selectionColor"
  val PanelSelectionBgColor = s"$filePanelCategory.selectionBgColor"
  val PanelCursorColor = s"$filePanelCategory.cursorColor"
  val PanelCursorBgColor = s"$filePanelCategory.cursorBgColor"

  val TransferBufferSize = "transfer.bufferSize"

  // runtime
  val windowCategory = "runtime.window"
  val WindowHeight = s"$windowCategory.height"
  val WindowWidth = s"$windowCategory.width"

  val Bookmarks = s"runtime.$generalCategory.bookmarks"
  val History = s"runtime.$generalCategory.history"

  val filePanelColumnCategory = s"runtime.$filePanelCategory.column"

  val NameColumnWidth = s"$filePanelColumnCategory.name.width"
  val ExtColumnWidth = s"$filePanelColumnCategory.extension.width"
  val SizeColumnWidth = s"$filePanelColumnCategory.size.width"
  val ModifiedColumnWidth = s"$filePanelColumnCategory.modified.width"
  val AttribsColumnWidth = s"$filePanelColumnCategory.attribs.width"
}
