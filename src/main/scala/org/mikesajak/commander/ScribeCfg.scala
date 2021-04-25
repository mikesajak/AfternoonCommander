package org.mikesajak.commander

import scribe.format.{Formatter, FormatterInterpolator, date, levelColored, mdc, message, positionAbbreviated, string}
import scribe.{Level, format}

object ScribeCfg {
  lazy val compactAbbreviatedFormatter: Formatter =
    formatter"$date ${string("[")}$levelColored${string("]")} ${format.green(positionAbbreviated)} - $message$mdc"

  def initScribeLogging(): Unit = {
    scribe.Logger.root
          .clearHandlers()
          .clearModifiers()
          .withHandler(minimumLevel = Some(Level.Trace), formatter = compactAbbreviatedFormatter)
          .replace()
  }
}
