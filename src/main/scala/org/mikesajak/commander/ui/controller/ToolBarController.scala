package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.config.Configuration

import scalafx.event.ActionEvent
import scalafx.scene.control.ToggleButton
import scalafxml.core.macros.sfxml

@sfxml
class ToolBarController(showHiddenToggleButton: ToggleButton,
                        config: Configuration) {

  showHiddenToggleButton.selected = config.boolProperty("file_panel", "show_hidden").getOrElse(false)

  def handleShowHiddenToggle(event: ActionEvent): Unit = {
    config.boolProperty("file_panel", "show_hidden") := showHiddenToggleButton.isSelected
  }
}

