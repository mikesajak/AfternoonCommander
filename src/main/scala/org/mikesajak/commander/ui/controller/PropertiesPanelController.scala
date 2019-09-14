package org.mikesajak.commander.ui.controller

import com.typesafe.scalalogging.Logger
import javafx.concurrent.Worker.State
import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.controller.properties._
import org.mikesajak.commander.ui.{IconResolver, IconSize, ResourceManager, UILoader}
import org.mikesajak.commander.util.Throttler
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.control.{Label, Tab, TabPane}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.BorderPane
import scalafxml.core.macros.{nested, sfxml}

trait PropertiesPanelController {
  def init(path: VPath): BackgroundService[(DirStats, DirContents)]
}

@sfxml
class PropertiesPanelControllerImpl(nameLabel: Label,
                                    iconLabel: Label,
                                    pathLabel: Label,

                                    statusMessageLabel: Label,
                                    statusDetailMessageLabel: Label,
                                    propertyPanelsTabPane: TabPane,

                                    @nested[GeneralPropertiesPanelControllerImpl] generalPropertiesPanelController: GeneralPropertiesPanelController,

                                    fileTypeManager: FileTypeManager,
                                    iconResolver: IconResolver,
                                    resourceMgr: ResourceManager)
    extends PropertiesPanelController {
  private val logger = Logger[PropertiesPanelControllerImpl]

  private val fileContentsLayout = "/layout/properties/file-content-properties-panel.fxml"
  private val dirContentsLayout = "/layout/properties/dir-content-properties-panel.fxml"

  override def init(path: VPath): BackgroundService[(DirStats, DirContents)] = {
    nameLabel.text = path.name

    iconLabel.text = ""
    iconLabel.graphic = iconResolver.findIconFor(path, IconSize(48)).orNull

    pathLabel.text = path.absolutePath

    val statsService = new BackgroundService(
      new DirWalkerTask(Seq(path), new DirStatsAndContentsProcessor(fileTypeManager)))

    prepareStatsUI(path, statsService)

    generalPropertiesPanelController.init(path, statsService)
    prepareAccessRightsTab(path)
    prepareContentTab(path, statsService)

    statsService
  }

  private def prepareStatsUI(path: VPath, statsService: BackgroundService[(DirStats, DirContents)]): Unit = {
    statusMessageLabel.text = null
    statusDetailMessageLabel.text = null

    val msgThrottler = new Throttler[String](50, str => Platform.runLater(statusDetailMessageLabel.text = str))
    Throttler.registerCancelOnServiceFinish(statsService, msgThrottler)
    statsService.message.onChange { (_, _, msg) => msgThrottler.update(msg) }

    statsService.state.onChange { (_, _, state) => state match {
      case State.RUNNING =>   notifyStarted()
      case State.FAILED =>    notifyError(Option(statsService.value.value._1), statsService.message.value)
      case State.SUCCEEDED => notifyFinished(statsService.value.value._1, None)
      case _ =>
    }}
  }

  private def prepareContentTab(path: VPath, statsService: BackgroundService[(DirStats, DirContents)]): Unit = {
    val (title, contentPane) = path match {
      case f: VFile =>
        val (pane, ctrl) = UILoader.loadScene[FileContentPropertiesPanelController](fileContentsLayout)
        ctrl.init(f, statsService)
        ("File contents", pane)
      case d: VDirectory =>
        val (pane, ctrl) = UILoader.loadScene[DirContentPropertiesPanelController](dirContentsLayout)
        ctrl.init(d, statsService)
        ("Directory contents", pane)
    }

    propertyPanelsTabPane += new Tab() {
      text = title
      content = contentPane
    }
  }

  private def prepareAccessRightsTab(path: VPath): Unit = {
    propertyPanelsTabPane += new Tab() {
      text = "Access rights"
      content = new BorderPane() {
        top = new Label("Access rights -> TODO")
      }
    }
  }

  def notifyStarted(): Unit = {
    logger.debug(s"notifyStarted...")
    statusMessageLabel.graphic = new ImageView(resourceMgr.getIcon("loading-chasing-arrows.gif"))
    statusMessageLabel.text = resourceMgr.getMessage("stats_panel.counting.message.label")
  }

  def notifyFinished(stats: DirStats, message: Option[String] = None): Unit = {
    logger.debug(s"notifyFinished: $stats, $message")

    statusMessageLabel.graphic = null
    statusMessageLabel.text = null

    statusDetailMessageLabel.graphic = null
    statusDetailMessageLabel.text = null
  }

  def notifyError(stats: Option[DirStats], message: String): Unit = {
    statusMessageLabel.graphic = null
    statusMessageLabel.text = message

    statusDetailMessageLabel.graphic = null
    statusDetailMessageLabel.text = null
  }
}
