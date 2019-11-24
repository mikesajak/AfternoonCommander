package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import scalafx.event.ActionEvent
import scalafx.scene.control.ToggleButton
import scalafxml.core.macros.sfxml

@sfxml
class ToolBarController(showHiddenToggleButton: ToggleButton,
                        config: Configuration) {

  showHiddenToggleButton.selected = config.boolProperty(ConfigKeys.ShowHiddenFiles).getOrElse(false)

  def handleShowHiddenToggle(event: ActionEvent): Unit = {
    config.boolProperty(ConfigKeys.ShowHiddenFiles) := showHiddenToggleButton.isSelected
  }
}

