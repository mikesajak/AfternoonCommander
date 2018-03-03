package org.mikesajak.commander

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.{FilesystemsManager, VDirectory}

class BookmarkMgr(config: Configuration) {
  private val logger = Logger[BookmarkMgr]
  private var bookmarks0 = Seq[VDirectory]()

  def init(fsMgr: FilesystemsManager): Unit = {
    val savedBookmarks = config.stringSeqProperty("general", "bookmarks").value

    logger.info(s"Read saved bookmarks: $savedBookmarks")

    bookmarks0 = savedBookmarks.map(b =>
      b.flatMap(bookmarkPath => fsMgr.resolvePath(bookmarkPath)
                                     .map(_.directory)))
                               .getOrElse(Seq())
  }

  def bookmarks: Seq[VDirectory] = bookmarks0

  def addBookmark(dir: VDirectory): Unit = {
    if (bookmarks0.contains(dir)) {
      logger.info(s"Bookmark already exists, not adding. Directory=$dir")
    } else {
      bookmarks0 :+= dir
      config.stringSeqProperty("general", "bookmarks") := bookmarks0.map(_.toString)
    }
  }

  def clear(): Unit = {
    logger.debug("Clearing all bookmarks")
    bookmarks0 = Seq()
    config.stringSeqProperty("general", "bookmarks") := Seq()
  }
}
