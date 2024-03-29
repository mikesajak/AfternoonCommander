package org.mikesajak.commander

import com.google.inject.Key
import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import org.mikesajak.commander.ui.{ResourceManager, UILoader}
import scalafx.Includes._
import scalafx.animation.{KeyFrame, KeyValue, Timeline}
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene
import scalafx.util.Duration
import scribe.Logging
import scribe.output._
/**
 * Created by mike on 25.10.14.
 */
object Main extends JFXApp3 with Logging {
  private val mainPanelDef: String = "/layout/main-panel-layout.fxml"

  ScribeCfg.initScribeLogging()

  override def start(): Unit = {
    logger.info(s"AfternoonCommander starting")
    logger.debug(out(green("test")))

    val injector = ApplicationContext.globalInjector.createChildInjector()
    val config = injector.getInstance(classOf[Configuration])
    val appController = injector.getInstance(classOf[ApplicationController])
    val resourceMgr = injector.getInstance(Key.get(classOf[ResourceManager]))
    val pluginManager = injector.getInstance(Key.get(classOf[PluginManager]))

    val (root, _) = UILoader.loadScene(mainPanelDef)

    stage = new PrimaryStage() {
      title = resourceMgr.getMessage("app.name")
      icons += resourceMgr.getIcon("internal_drive.png")
      scene = new Scene(root)
    }

    //  Platform.implicitExit = false
    stage.onCloseRequest = we => {
      we.consume()
      appController.exitApplication { () =>
        new Timeline {
          keyFrames.add(KeyFrame(Duration(800), "fadeOut", null, Set(KeyValue(stage.opacity, 0))))
          onFinished = () => Platform.exit()
        }.play()
        true
      }
    }

    appController.init(stage, this)
    pluginManager.init()

    stage.width = config.intProperty(ConfigKeys.WindowWidth).getOrElse(1000): Int
    stage.height = config.intProperty(ConfigKeys.WindowHeight).getOrElse(600): Int

    stage.toFront()
    stage.opacity.value = 0
    stage.show()

    new Timeline {
      keyFrames.add(KeyFrame(Duration(800), "fadeIn", null, Set(KeyValue(stage.opacity, 1))))
    }.play()
  }

  override def main(args: Array[String]): Unit = {
    super.main(args)
  }
}

