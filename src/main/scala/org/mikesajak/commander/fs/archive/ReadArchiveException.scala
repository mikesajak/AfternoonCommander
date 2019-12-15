package org.mikesajak.commander.fs.archive

class ReadArchiveException(message: String) extends Exception(message) {
  def this(message: String, cause: Throwable) = {
    this(message)
    initCause(cause)
  }
}
