package org.mikesajak.commander.status

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController

class OperationMgr(statusMgr: StatusMgr, appController: ApplicationController) {
  private val logger = Logger(this.getClass)

  def handleView(): Unit = {
    logger.warn(s"handleView - Not implemented yet!")
  }

  def handleEdit(): Unit = {
    logger.warn(s"handleEdit - Not implemented yet!")
  }

  def handleCopy(): Unit = {
    logger.warn(s"handleCopy - Not implemented yet!")
  }

  def handleMove(): Unit = {
    logger.warn(s"handleMove - Not implemented yet!")
  }

  def handleMkDir(): Unit = {
    logger.warn(s"handleMkDir - Not implemented yet!")
    val curTab = statusMgr.selectedTabManager.selectedTab
    logger.debug(s"handleMkDir - curTab=$curTab")
  }

  def handleDelete(): Unit = {
    logger.warn(s"handleDelete - Not implemented yet!")
  }

  def handleExit(): Unit = {
    appController.exitApplication()
  }

}
