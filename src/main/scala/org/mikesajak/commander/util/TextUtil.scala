package org.mikesajak.commander.util

object TextUtil {
  private val UpperPat = "[A-Z\\d]".r

  def camelToSnake(txt: String): String =
    UpperPat.replaceAllIn(txt, { m =>
      if (m.end(0) == 1) m.group(0).toLowerCase
      else "_" + m.group(0).toLowerCase
    })
}
