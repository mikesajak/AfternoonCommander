package org.mikesajak.commander.ui.controller

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
import scalafxml.core.macros.{nested, sfxml}
import scribe.Logging

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
                                    resourceMgr: ResourceManager,
                                    serviceRegistry: BackgroundServiceRegistry)
    extends PropertiesPanelController with Logging {

  private val layoutsDir = "/layout/properties"

  private val fileContentsLayout = s"$layoutsDir/file-content-properties-panel.fxml"
  private val accessPermissionsLayout = s"$layoutsDir/access-permissions-properties-panel.fxml"
  private val dirContentsLayout = s"$layoutsDir/dir-content-properties-panel.fxml"
  private val dirSizeTreeLayout = s"$layoutsDir/dir-size-tree-properties-panel.fxml"

  override def init(path: VPath): Seq[BackgroundService[_]] = {
    nameLabel.text = path.name

    iconLabel.text = ""
    iconLabel.graphic = iconResolver.findIconFor(path, IconSize(48)).orNull

    pathLabel.text = path.absolutePath

    val statsService = serviceRegistry.registerServiceFor(new DirWalkerTask(Seq(path),
                                                                            new DirStatsAndContentsProcessor(fileTypeManager)))

    prepareStatsUI(statsService)

    generalPropertiesPanelController.init(path, statsService)
    prepareAccessRightsTab(path)
    val contentService = prepareContentTab(path, statsService)

    val services = Seq(Some(statsService), contentService).flatten

    statsService.state.onChange { (_, _, state) =>
      state match {
        case State.RUNNING =>
          notifyStarted()
        case State.FAILED =>
          notifyError(statsService.message.value)
        case State.SUCCEEDED =>
          notifyFinished()
        case _ =>
      }
    }

    services
  }

  private def prepareStatsUI(statsService: BackgroundService[(String, DirStats, DirContents)]): Unit = {
    statusMessageLabel.text = null
    statusDetailMessageLabel.text = null

    val msgThrottler = new Throttler[String](50, str => Platform.runLater(statusDetailMessageLabel.text = str))
    Throttler.registerCancelOnServiceFinish(statsService, msgThrottler)
    statsService.message.onChange { (_, _, msg) => msgThrottler.update(msg) }
  }

  private def prepareContentTab(path: VPath, statsService: BackgroundService[(String, DirStats, DirContents)]): Option[BackgroundService[_]] = {
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
    val (pane, ctrl) = UILoader.loadScene[AccessPermissionsPropertiesPanelController](accessPermissionsLayout)
    ctrl.init(path)

    propertyPanelsTabPane += new Tab() {
      text = resourceMgr.getMessage("properties_panel.access_rights_tab.title")
      content = pane
    }
  }

  def notifyStarted(): Unit = {
    logger.debug(s"notifyStarted...")
    statusMessageLabel.graphic = new ImageView(resourceMgr.getIcon("loading-chasing-arrows.gif"))
    statusMessageLabel.text = resourceMgr.getMessage("properties_panel.general_tab.status.analyze")
  }

  def notifyFinished(): Unit = {
    statusMessageLabel.graphic = null
    statusMessageLabel.text = null
    statusMessageLabel.visible = false
    statusMessageLabel.maxHeight = 0

    statusDetailMessageLabel.graphic = null
    statusDetailMessageLabel.text = null
    statusDetailMessageLabel.visible = false
    statusDetailMessageLabel.maxHeight = 0
  }

  def notifyError(message: String): Unit = {
    statusMessageLabel.graphic = null
    statusMessageLabel.text = message

    statusDetailMessageLabel.graphic = null
    statusDetailMessageLabel.text = null
  }
}
