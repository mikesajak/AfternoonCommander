package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.OperationMgr
import scalafxml.core.macros.sfxml

/**
  * Created by mike on 09.04.17.
  */
@sfxml
class ButtonBarController(opsMgr: OperationMgr) {
  def handleView(): Unit = opsMgr.handleView()

  def handleEdit(): Unit = opsMgr.handleEdit()

  def handleCopy(): Unit = opsMgr.handleCopy()

  def handleMove(): Unit = opsMgr.handleMove()

  def handleMkDir(): Unit = opsMgr.handleMkDir()

  def handleDelete(): Unit = opsMgr.handleDelete()

  def handleExit(): Unit = opsMgr.handleExit()
}
