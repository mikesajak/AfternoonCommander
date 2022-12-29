package org.mikesajak.commander.util

import java.io.BufferedReader

object ReadLineIterator {
  implicit class BufferedReaderOps(reader: BufferedReader) {
    def lineIterator = new ReadLineIterator(reader)
  }
}

class ReadLineIterator(reader: BufferedReader) extends Iterator[String] {
  private var cachedLine: String = _
  private var streamEnd = false

  override def hasNext: Boolean = {
    prepareLine()
    !streamEnd || cachedLine != null
  }

  override def next(): String = {
    prepareLine()
    val line = getNextLine
    if (line == null)
      throw new IllegalStateException("There is no next line")
    line
  }

  private def getNextLine: String = {
    prepareLine()
    val line = cachedLine
    cachedLine = null
    line
  }


  private def prepareLine(): Unit = {
    if (!streamEnd && cachedLine == null) {
      cachedLine = reader.readLine()
      streamEnd = cachedLine == null
    }
  }
}