package org.mikesajak.commander.util

object TextUtils {

  def shortenPathBy(path: String, length: Option[Int] = None): String = {
    val wishLen = length.getOrElse(10)
    shortenPathTo(path, wishLen+3)
  }

  def shortenPathTo(path: String, length: Int) =
    if (path.length <= length) path
    else {
      val wishLength = math.min(length + 3, path.length - 4)
      val cutLength = path.length - wishLength
      s"...${path.substring(cutLength)}"
    }
}
