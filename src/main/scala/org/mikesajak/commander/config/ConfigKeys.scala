package org.mikesajak.commander.config

object ConfigKeys {
  val WindowHeight: ConfigKey = ConfigKey("window", "height")
  val WindowWidth: ConfigKey = ConfigKey("window", "width")

  val ExitConfirmation: ConfigKey = ConfigKey("application", "exitConfirmation")

  val ShowHiddenFiles: ConfigKey = ConfigKey("file_panel", "show_hidden")

  val Bookmarks: ConfigKey = ConfigKey("general", "bookmarks")

  val TransferBufferSize: ConfigKey = ConfigKey("transfer", "buffer_size")

}
