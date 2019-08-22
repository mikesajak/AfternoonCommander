package org.mikesajak.commander.handler

import java.awt.Desktop
import java.io.IOException
import java.util.concurrent.Executors

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.fs.local.LocalFile
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class DefaultOSActionFileHandler(override val path: VPath, appCtrl: ApplicationController) extends ActionFileHandler(path) {
  private val logger = Logger[DefaultOSActionFileHandler]

  private implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  override def handle(): Unit = try {
    if (Desktop.isDesktopSupported) path match {
      case file: LocalFile => Future {
        try {
          logger.debug(s"Executing default (defined in desktop) action for file $file")
          Desktop.getDesktop.open(file.file)
          logger.debug(s"Finished Executing default (defined in desktop) action for file $file")
        } catch {
          case e: Exception => logger.warn(s"An error occurred while executing (desktop) action for file $file")
        }
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
}
