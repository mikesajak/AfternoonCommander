package org.mikesajak.commander.ui.controller.properties

import com.typesafe.scalalogging.Logger
import javafx.concurrent.Worker.State
import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.task.{BackgroundService, DirContents, DirStats}
import org.mikesajak.commander.ui.controller.FileRow
import org.mikesajak.commander.util.Throttler
import org.mikesajak.commander.{FileType, FileTypeManager}
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.chart.PieChart
import scalafx.scene.control.{TableColumn, TableView}
import scalafxml.core.macros.sfxml

trait DirContentPropertiesPanelController {
  def init(dir: VDirectory, statsService: BackgroundService[(DirStats, DirContents)])
}

class ExtensionTableRow(entry: (String, Int)) {
  val name = new StringProperty(entry._1)
  val value = new StringProperty(entry._2.toString)

  override def toString: String = s"ExtensionTableRow(${entry._1}, ${entry._2})"

  def canEqual(other: Any): Boolean = other.isInstanceOf[ExtensionTableRow]

  override def equals(other: Any): Boolean = other match {
    case that: FileRow => that canEqual this
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(entry)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

@sfxml
class DirContentPropertiesPanelControllerImpl(fileTypesPieChart: PieChart,
                                              extensionTableView: TableView[ExtensionTableRow],
                                              extensionColumn: TableColumn[ExtensionTableRow, String],
                                              countColumn: TableColumn[ExtensionTableRow, String],
                                              fileTypeManager: FileTypeManager)
    extends DirContentPropertiesPanelController {
  private val logger = Logger[DirContentPropertiesPanelControllerImpl]

  override def init(dir: VDirectory, statsService: BackgroundService[(DirStats, DirContents)]): Unit = {
    fileTypesPieChart.data = Seq(
      PieChart.Data("Files", 1)
    )

    extensionColumn.cellValueFactory = { _.value.name }
    countColumn.cellValueFactory = { _.value.value }

    val throttler = new Throttler[DirContents](200, value => Platform.runLater(updateChart(value)))
    statsService.value.onChange { (_, _, state) => throttler.update(state._2) }

    statsService.state.onChange { (_, _, state) => state match {
      case State.SUCCEEDED =>
        throttler.cancel()
        updateChart(statsService.value.value._2)
      case State.CANCELLED | State.FAILED =>
        throttler.cancel()
      case _ =>
    }}
  }

  private def updateChart(contents: DirContents): Unit = {
    val pieChartSegments = contents.typesMap.toSeq
                        .sortWith(compareByNameAndCount)
                        .map(entry => PieChart.Data(s"${fileTypeManager.descriptionOf(entry._1)} (${entry._2})", entry._2))

    fileTypesPieChart.data = pieChartSegments

    val tableRows = contents.extensionsMap.toSeq
        .sortWith(compareByNameAndCount2)
        .map(entry => new ExtensionTableRow(entry))

    extensionTableView.items = ObservableBuffer(tableRows)
  }

  private def compareByNameAndCount(entry1: (FileType, Int), entry2: (FileType, Int)): Boolean = {
    val delta = (entry1._2 - entry2._2) match {
      case 0 => fileTypeManager.descriptionOf(entry1._1) compareTo fileTypeManager.descriptionOf(entry2._1)
      case x => x
    }
    delta >= 0
  }

  private def compareByNameAndCount2(entry1: (String, Int), entry2: (String, Int)): Boolean = {
    val delta = (entry1._2 - entry2._2) match {
      case 0 => entry1._1 compareTo entry2._1
      case x => x
    }
    delta >= 0
  }
}
