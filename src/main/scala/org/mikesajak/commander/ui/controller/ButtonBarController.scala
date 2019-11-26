package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.OperationMgr
import scalafx.scene.control.Button
import scalafxml.core.macros.sfxml

/**
  * Created by mike on 09.04.17.
  */
@sfxml
class ButtonBarController(viewButton: Button,
                          editButton: Button,
                          copyButton: Button,
                          moveButton: Button,
                          mkdirButton: Button,
                          deleteButton: Button,
                          exitButton: Button,

                          opsMgr: OperationMgr) {
  // Disable not implemented buttons...
  viewButton.disable = true
  editButton.disable = true

  def handleView(): Unit = opsMgr.handleView()

  def handleEdit(): Unit = opsMgr.handleEdit()

  def handleCopy(): Unit = opsMgr.handleCopy()

  def handleMove(): Unit = opsMgr.handleMove()

  def handleMkDir(): Unit = opsMgr.handleMkDir()

  def handleDelete(): Unit = opsMgr.handleDelete()

  def handleExit(): Unit = opsMgr.handleExit()
}
