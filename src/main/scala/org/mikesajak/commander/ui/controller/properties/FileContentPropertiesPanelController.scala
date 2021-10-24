package org.mikesajak.commander.ui.controller.properties

import javafx.{concurrent => jfxconcur}
import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.VFile
import org.mikesajak.commander.task.BackgroundService
import org.mikesajak.commander.ui.controller.FileRow
import scalafx.Includes._
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{TableColumn, TableView}
import scalafxml.core.macros.sfxml
import scribe.Logging

trait FileContentPropertiesPanelController {
  def init(path: VFile): BackgroundService[Map[String, Seq[String]]]
}

@sfxml
class FileContentPropertiesPanelControllerImpl(metadataTableView: TableView[MetadataRow],
                                               metadataNameColumn: TableColumn[MetadataRow, String],
                                               metadataValueColumn: TableColumn[MetadataRow, String],

                                               fileTypeManager: FileTypeManager)
    extends FileContentPropertiesPanelController with Logging {

  override def init(path: VFile): BackgroundService[Map[String, Seq[String]]] = {
    metadataNameColumn.cellValueFactory = { _.value.name }
    metadataNameColumn.prefWidth <== metadataTableView.width.multiply(0.3)
    metadataValueColumn.cellValueFactory = { _.value.value }
    metadataValueColumn.prefWidth <== metadataTableView.width.multiply(0.7)

    val duration = 500.ms

    val timeline = new Timeline {
      keyFrames = (0 to 3)
          .map(frameNum => KeyFrame(duration * frameNum, "Parse progress ticker",
                                    _ => metadataTableView.items = ObservableBuffer(new MetadataRow(s"Parsing file${"." * frameNum}", Seq()))))
      cycleCount = Timeline.Indefinite
      delay = duration
    }

    new BackgroundService(new jfxconcur.Task[Map[String, Seq[String]]]() {
      def call(): Map[String, Seq[String]] = try {
        timeline.play()
        fileTypeManager.metadataOf(path)
      } finally {
        timeline.stop()
      }

      override def done(): Unit = {
        val tableRows = get().toSeq
                             .map(entry => new MetadataRow(entry))
        logger.debug(s"File content properties service finished.\nResult=$tableRows")

        metadataTableView.items = ObservableBuffer(tableRows.sortBy(_.name.value.toLowerCase): _*)
      }
    })
  }
}

class MetadataRow(entry: (String, Seq[String])) {
  val name = new StringProperty(entry._1)
  val value = new StringProperty(entry._2.reduceLeftOption((acc, elem) => s"$acc, $elem").orNull)

  override def toString: String = s"MetadataRow(${entry._1}, ${entry._2})"

  def canEqual(other: Any): Boolean = other.isInstanceOf[MetadataRow]

  override def equals(other: Any): Boolean = other match {
    case that: FileRow => that canEqual this
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(entry)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
