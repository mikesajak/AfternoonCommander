package org.mikesajak.commander.ui.controller

import com.typesafe.scalalogging.Logger
import javafx.concurrent.Worker
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
  def init(path: VPath): Seq[BackgroundService[_]]
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

  override def init(path: VPath): Seq[BackgroundService[_]] = {
    nameLabel.text = path.name

    iconLabel.text = ""
    iconLabel.graphic = iconResolver.findIconFor(path, IconSize(48)).orNull

    pathLabel.text = path.absolutePath

    val statsService = new BackgroundService(
      new DirWalkerTask(Seq(path), new DirStatsAndContentsProcessor(fileTypeManager)))

    prepareStatsUI(path, statsService)

    generalPropertiesPanelController.init(path, statsService)
    prepareAccessRightsTab(path)
    val contentService = prepareContentTab(path, statsService)

    val services = Seq(Some(statsService), contentService).flatten

    services.foreach { srv =>
      srv.state.onChange { (_, _, state) =>
        state match {
          case State.RUNNING => notifyStarted()
          case State.FAILED => notifyError(Option(statsService.value.value._1), statsService.message.value)
          case State.SUCCEEDED => notifyFinished(statsService.value.value._1, services)
          case _ =>
        }
      }
    }

    services
  }

  private def prepareStatsUI(path: VPath, statsService: BackgroundService[(DirStats, DirContents)]): Unit = {
    statusMessageLabel.text = null
    statusDetailMessageLabel.text = null

    val msgThrottler = new Throttler[String](50, str => Platform.runLater(statusDetailMessageLabel.text = str))
    Throttler.registerCancelOnServiceFinish(statsService, msgThrottler)
    statsService.message.onChange { (_, _, msg) => msgThrottler.update(msg) }
  }

  private def prepareContentTab(path: VPath, statsService: BackgroundService[(DirStats, DirContents)]): Option[BackgroundService[_]] = {
    val (title, contentPane, tabService) = path match {
      case f: VFile =>
        val (pane, ctrl) = UILoader.loadScene[FileContentPropertiesPanelController](fileContentsLayout)
        val srv = ctrl.init(f)
        (resourceMgr.getMessage("properties_panel.file_contents_tab.title"), pane, Some(srv))
      case d: VDirectory =>
        val (pane, ctrl) = UILoader.loadScene[DirContentPropertiesPanelController](dirContentsLayout)
        ctrl.init(d, statsService)
        (resourceMgr.getMessage("properties_panel.dir_contents_tab.title"), pane, None)
    }

    propertyPanelsTabPane += new Tab() {
      text = title
      content = contentPane
    }

    tabService
  }

  private def prepareAccessRightsTab(path: VPath): Unit = {
    propertyPanelsTabPane += new Tab() {
      text = resourceMgr.getMessage("properties_panel.access_rights_tab.title")
      content = new BorderPane() {
        top = new Label("Access rights -> TODO")
      }
    }
  }

  def notifyStarted(): Unit = {
    logger.debug(s"notifyStarted...")
    statusMessageLabel.graphic = new ImageView(resourceMgr.getIcon("loading-chasing-arrows.gif"))
    statusMessageLabel.text = resourceMgr.getMessage("properties_panel.general_tab.status.analyze")
  }

  def notifyFinished(stats: DirStats, services: Seq[BackgroundService[_]]): Unit = {
    if (services.forall(srv => isFinishedState(srv.state.value))) {
      statusMessageLabel.graphic = null
      statusMessageLabel.text = null
      statusMessageLabel.visible = false
      statusMessageLabel.maxHeight = 0

      statusDetailMessageLabel.graphic = null
      statusDetailMessageLabel.text = null
      statusDetailMessageLabel.visible = false
      statusDetailMessageLabel.maxHeight = 0
    }
  }

  private def isFinishedState(state: Worker.State): Boolean = state match {
    case State.SUCCEEDED | State.CANCELLED | State.FAILED => true
    case _ => false
  }

  def notifyError(stats: Option[DirStats], message: String): Unit = {
    statusMessageLabel.graphic = null
    statusMessageLabel.text = message

    statusDetailMessageLabel.graphic = null
    statusDetailMessageLabel.text = null
  }
}
