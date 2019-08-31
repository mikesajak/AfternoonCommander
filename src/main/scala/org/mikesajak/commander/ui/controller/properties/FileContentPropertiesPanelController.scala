package org.mikesajak.commander.ui.controller.properties

import com.typesafe.scalalogging.Logger
import javafx.{concurrent => jfxconcur}
import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.VFile
import org.mikesajak.commander.task.BackgroundService
import org.mikesajak.commander.ui.controller.FileRow
import scalafx.Includes._
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{TableColumn, TableView}
import scalafxml.core.macros.sfxml

trait FileContentPropertiesPanelController {
  def init(path: VFile)
}

@sfxml
class FileContentPropertiesPanelControllerImpl(metadataTableView: TableView[MetadataRow],
                                               metadataNameColumn: TableColumn[MetadataRow, String],
                                               metadataValueColumn: TableColumn[MetadataRow, String],

                                               fileTypeManager: FileTypeManager)
    extends FileContentPropertiesPanelController {
  private val logger = Logger[FileContentPropertiesPanelControllerImpl]

  override def init(path: VFile): Unit = {
    metadataNameColumn.cellValueFactory = { _.value.name }
    metadataNameColumn.prefWidth <== metadataTableView.width.multiply(0.3)
    metadataValueColumn.cellValueFactory = { _.value.value }
    metadataValueColumn.prefWidth <== metadataTableView.width.multiply(0.7)

    new BackgroundService(new jfxconcur.Task[Map[String, Seq[String]]]() {
      def call(): Map[String, Seq[String]] = fileTypeManager.metadataOf(path)

      override def done(): Unit = {
        val tableRows = get().toSeq
                             .map(entry => new MetadataRow(entry))

        metadataTableView.items = ObservableBuffer(tableRows.sortBy(_.name.value.toLowerCase))
      }
    }).start()
  }
}

class ReadMetadataTask(file: VFile, fileTypeManager: FileTypeManager) extends javafx.concurrent.Task[Map[String, Seq[String]]] {
  override def call(): Map[String, Seq[String]] =
    fileTypeManager.metadataOf(file)

  override def succeeded(): Unit = {

  }
}

class MetadataRow(entry: (String, Seq[String])) {
  val name = new StringProperty(entry._1)
  val value = new StringProperty(entry._2.reduceLeft((acc, elem) => s"$acc, $elem"))

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
