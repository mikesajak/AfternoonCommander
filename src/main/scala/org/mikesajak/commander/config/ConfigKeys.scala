package org.mikesajak.commander.config

object ConfigKeys {
  val WindowHeight: ConfigKey = ConfigKey("window", "height")
  val WindowWidth: ConfigKey = ConfigKey("window", "width")

  val ExitConfirmation: ConfigKey = ConfigKey("application", "exitConfirmation")

  val ShowHiddenFiles: ConfigKey = ConfigKey("filePanel", "showHidden")

  val Bookmarks: ConfigKey = ConfigKey("general", "bookmarks")

  val TransferBufferSize: ConfigKey = ConfigKey("transfer", "bufferSize")

  val filePanelColumnCategory = "filePanel.column"
  val NameColumnWidth: ConfigKey = ConfigKey(filePanelColumnCategory, "name.width")
  val ExtColumnWidth: ConfigKey = ConfigKey(filePanelColumnCategory, "extension.width")
  val SizeColumnWidth: ConfigKey = ConfigKey(filePanelColumnCategory, "size.width")
  val ModifiedColumnWidth: ConfigKey = ConfigKey(filePanelColumnCategory, "modified.width")
  val AttribsColumnWidth: ConfigKey = ConfigKey(filePanelColumnCategory, "attribs.width")

}
