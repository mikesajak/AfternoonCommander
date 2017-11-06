package org.mikesajak.commander

import com.google.inject._
import com.google.inject.name.{Named, Names}
import com.typesafe.scalalogging.Logger
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.{Configuration, TypesafeConfig}
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}
import org.mikesajak.commander.ui.controller.{DirTabManager, PanelId}
import org.mikesajak.commander.ui.{CountDirStatsOperationCtrl, DeleteOperationCtrl, MkDirOperationCtrl, ResourceManager}

/**
  * Created by mike on 09.04.17.
  */
class ApplicationContext extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    install(new PanelContext(LeftPanel))
    install(new PanelContext(RightPanel))

    install(new UIOperationControllersContext)
  }

  @Provides
  @Singleton
  def provideConfig(): Configuration = {
    val config = new TypesafeConfig(s"${ApplicationController.configPath}/${ApplicationController.configFile}")
    config.load()
    config
  }

  @Provides
  @Singleton
  def provideAppController(config: Configuration) = new ApplicationController(config)

  @Provides
  @Singleton
  def provideFileTypeManager(archiveManager: ArchiveManager) = new FileTypeManager(archiveManager)

  @Provides
  @Singleton
  def provideArchiveManager() = new ArchiveManager()

  @Provides
  @Singleton
  def provideFilesystemsManager(): FilesystemsManager = {
    val fsMgr = new FilesystemsManager()
    fsMgr.init()
    fsMgr
  }

  @Provides
  @Singleton
  def providePluginManager(filesystemsManager: FilesystemsManager,
                           archiveManager: ArchiveManager): PluginManager = {
    val pluginMgr = new PluginManager(filesystemsManager, archiveManager)
    pluginMgr.init()
    pluginMgr
  }

  @Provides
  @Singleton
  def provideStatusManager(@Named("LeftPanel") leftDirTabMgr: DirTabManager,
                           @Named("RightPanel") rightDirTabMgr: DirTabManager): StatusMgr = {
    new StatusMgr(leftDirTabMgr, rightDirTabMgr)
  }

  @Provides
  @Singleton
  def provideTaskManager(): TaskManager = {
    new TaskManager
  }

  @Provides
  @Singleton
  def provideOperationManager(statusMgr: StatusMgr, resourceMgr: ResourceManager,
                              fsMgr: FilesystemsManager, taskManager: TaskManager,
                              appController: ApplicationController,
                              mkDirOperationCtrl: MkDirOperationCtrl,
                              deleteOperationCtrl: DeleteOperationCtrl,
                              countDirStatsOperationCtrl: CountDirStatsOperationCtrl): OperationMgr = {
    new OperationMgr(statusMgr, resourceMgr, fsMgr, taskManager, appController, mkDirOperationCtrl, deleteOperationCtrl, countDirStatsOperationCtrl)
  }
}

class PanelContext(panelId: PanelId) extends PrivateModule {//with ScalaModule {
  private val logger = Logger(this.getClass)

  override def configure(): Unit = {
    logger.trace(s"Configuring PanelContext for panelId=${panelId.toString}")
    bind(classOf[DirTabManager]).annotatedWith(Names.named(panelId.toString))
      //.to(classOf[DirTabManager])
      .toInstance(new DirTabManager(panelId)) // todo: use better way - some provider etc...
    expose(classOf[DirTabManager]).annotatedWith(Names.named(panelId.toString))
  }

  @Provides
  @Singleton
  def provideDirTabManager(): DirTabManager = {
    new DirTabManager(panelId)
  }
}

class UIOperationControllersContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  @Provides
  @Singleton
  def provideMkDirOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController) =
    new MkDirOperationCtrl(statusMgr, appController)

  @Provides
  @Singleton
  def provideDeleteOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                                 countDirOpCtrl: CountDirStatsOperationCtrl, resourceMgr: ResourceManager) =
    new DeleteOperationCtrl(statusMgr, appController, countDirOpCtrl, resourceMgr)

  @Provides
  @Singleton
  def provideCountDirStatsOperationCtrl(statusMgr: StatusMgr, taskManager: TaskManager, appController: ApplicationController) =
    new CountDirStatsOperationCtrl(statusMgr, taskManager, appController)
}

object ApplicationContext {
  val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}