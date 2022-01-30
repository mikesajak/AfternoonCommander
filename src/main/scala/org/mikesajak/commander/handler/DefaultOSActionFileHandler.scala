package org.mikesajak.commander.handler

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.local.LocalPath
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import scribe.Logging

import java.awt.Desktop
import java.io.IOException
import scala.concurrent.{ExecutionContextExecutor, Future}

class DefaultOSActionFileHandler(override val path: VPath, appCtrl: ApplicationController,
                                 executionContext: ExecutionContextExecutor)
    extends ActionFileHandler(path) with Logging {
  private implicit val ec: ExecutionContextExecutor = executionContext

  override def handle(): Unit = try {
    if (Desktop.isDesktopSupported) path match {
      case file: LocalPath =>
        Future {
          executeOSAction(file)
        }

      case file: VFile => logger.info(s"The file $file is not a local file, skipping desktop action.")

      case dir: VDirectory => logger.info(s"The file $dir is a directory. Skipping desktop action.")

      case file@_ => logger.info(s"Unknown file $file. Skipping desktop action.")
    }
    else logger.warn(s"Cannot execute default action for $path. Desktop action is not supported by OS.")
  } catch {
    case e: IOException =>
      logger.info(
        s"""Error while opening file $path by default OS/Desktop environment application.
           |Most probably there's no association defined for this file.""".stripMargin, e)
  }

  private def executeOSAction(file: LocalPath): Unit = {
    try {
      logger.debug(s"Executing default (defined in desktop) action for file $file")
      Desktop.getDesktop.open(file.file)
      logger.debug(s"Finished Executing default (defined in desktop) action for file $file")
    } catch {
      case e: Exception => logger.warn(s"An error occurred while executing (desktop) action for file $file. $e")
    }
  }
}
