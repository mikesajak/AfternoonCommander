package org.mikesajak.commander.util

object TextUtil {
  private val UpperPat = "[A-Z\\d]".r

  def camelToSnake(txt: String): String =
    UpperPat.replaceAllIn(txt, { m =>
      if (m.end(0) == 1) m.group(0).toLowerCase
      else "_" + m.group(0).toLowerCase
    })

  def containsIgnoreCase(str: String, searchStr: String): Boolean = {
    if (str == null || searchStr == null) return false

    val length = searchStr.length
    if (length == 0) return true

    var i = str.length - length

    while (i >= 0) {
      if (str.regionMatches(true, i, searchStr, 0, length))
        return true
      i -= 1
    }
    false
  }
}
