package org.mikesajak.commander

import scribe.Level
import scribe.format.{Formatter, FormatterInterpolator, date, green, levelColored, mdc, messages, positionAbbreviated, string}

object ScribeCfg {
  lazy val compactAbbreviatedFormatter: Formatter =
    formatter"$date ${string("[")}$levelColored${string("]")} ${ green(positionAbbreviated)} - $messages$mdc"

  def initScribeLogging(): Unit = {
    scribe.Logger.root
          .clearHandlers()
          .clearModifiers()
          .withHandler(minimumLevel = Some(Level.Trace), formatter = compactAbbreviatedFormatter)
          .replace()
  }
}
