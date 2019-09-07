package org.mikesajak.commander.task

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxc}
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.util.PathUtils

trait PathProcessor[A] {
  def title: String
  def process(files: Seq[VFile], dirs: Seq[VDirectory], level: Int): A

  def Empty: A
  def merge(res1: A, res2: A): A
}

class DirWalkerTask[A](paths: Seq[VPath], pathProcessor: PathProcessor[A]) extends jfxc.Task[A] {
  private val logger = Logger[DirWalkerTask[A]]

  updateTitle(s"${pathProcessor.title} ($paths)")

  override def call(): A = try {
    updateProgress(-1, -1) // set progress indeterminate

    val (dirs, files) = PathUtils.splitDirsAndFiles(paths)

    val result = dirs.foldLeft(pathProcessor.process(files, dirs, 0)) {
      (state, dir) => processDir(dir, state, 1)
    }

    updateProgress(1, 1)
    result
  } catch {
    case ce: CancelledException[A] =>
      logger.info(s"Task ${pathProcessor.title} has been cancelled.")
      updateMessage(s"Operation ${pathProcessor.title} has been cancelled.") // TODO: i18
      ce.value
    case e: Exception =>
      updateMessage(e.getLocalizedMessage)
      throw e
  }

  private def processDir(dir: VDirectory, curState: A, level: Int): A = {
    if (isCancelled)
      throw CancelledException(curState)

    updateMessage(dir.absolutePath)

    val curResult = pathProcessor.merge(curState,
                                        pathProcessor.process(dir.childFiles, dir.childDirs, level))
    val totalResult = (dir.childDirs foldLeft curResult) {
      (state, subDir) => processDir(subDir, state, level + 1)
    }

    updateValue(totalResult)
    totalResult
  }
}
