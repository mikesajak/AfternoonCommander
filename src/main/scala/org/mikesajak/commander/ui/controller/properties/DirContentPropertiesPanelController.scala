package org.mikesajak.commander.ui.controller.properties

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.VDirectory
import scalafx.scene.chart.PieChart
import scalafxml.core.macros.sfxml

trait DirContentPropertiesPanelController {
  def init(dir: VDirectory)
}

@sfxml
class DirContentPropertiesPanelControllerImpl(fileTypesPieChart: PieChart)
    extends DirContentPropertiesPanelController {
  private val logger = Logger[DirContentPropertiesPanelControllerImpl]

  override def init(dir: VDirectory): Unit = {
    fileTypesPieChart.data = Seq(
      PieChart.Data("jpg", 10),
      PieChart.Data("odt", 3),
      PieChart.Data("txt", 7),
      PieChart.Data("mkv", 35),
      PieChart.Data("mp3", 45)
    )

  }
}
