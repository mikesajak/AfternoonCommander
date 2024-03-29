package org.mikesajak.commander

import com.google.inject._
import com.google.inject.name.{Named, Names}
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.archive.ArchiveManager
import org.mikesajak.commander.config.{ConfigKeys, Configuration, TypesafeConfig}
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.handler.FileHandlerFactory
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.{BackgroundServiceRegistry, UserDecisionCtrl}
import org.mikesajak.commander.ui._
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}
import org.mikesajak.commander.ui.controller.{DirTabManager, PanelId}
import org.mikesajak.commander.ui.keys.{KeyActionLoader, KeyActionMapper}
import scribe.Logging

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.io.Source

/**
  * Created by mike on 09.04.17.
  */
class ApplicationContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new PanelContext(LeftPanel))
    install(new PanelContext(RightPanel))

    install(new UIOperationControllersContext)
  }

  @Provides
  @Singleton
  def provideConfig(eventBus: EventBus): Configuration = {
    val config = new TypesafeConfig(s"${ApplicationController.configPath}/${ApplicationController.configFile}",
                                    eventBus)
    config.load()
    setConfigDefaults(config)
    config
  }

  private def setConfigDefaults(config: Configuration): Unit = {
    config.stringProperty(ConfigKeys.PanelTextColor) := "#5D5D5D"
    config.stringProperty(ConfigKeys.PanelBgColor1) := "#FFFFFF"
    config.stringProperty(ConfigKeys.PanelBgColor2) := "#FFFFFF"
    config.stringProperty(ConfigKeys.PanelSelectionColor) := "#FFFFFF"
    config.stringProperty(ConfigKeys.PanelSelectionBgColor) := "#E95420"
    config.stringProperty(ConfigKeys.PanelCursorColor) := "#FFFFFF"
    config.stringProperty(ConfigKeys.PanelCursorBgColor) := "#4444FF"
  }

  @Provides
  @Singleton
  def provideKeyMapper(loader: KeyActionLoader): KeyActionMapper = {
    val mapping = loader.load(Source.fromResource("actions.keymap"))
    new KeyActionMapper(KeyActionMapper.Default ++ mapping)
  }

  @Provides
  @Singleton
  def provideKeyActionLoader() = new KeyActionLoader

  @Provides
  @Singleton
  def provideAppController(config: Configuration) = new ApplicationController(config)

  @Provides
  @Singleton
  def provideOSResolver = new OSResolver

  @Provides
  @Singleton
  def provideFileTypeManager(archiveManager: ArchiveManager, osResolver: OSResolver,
                             resourceMgr: ResourceManager, appController: ApplicationController) =
    new FileTypeManager(archiveManager, osResolver, resourceMgr, appController)

  @Provides
  @Singleton
  def provideArchiveManager(): ArchiveManager = new ArchiveManager()

  @Provides
  @Singleton
  def provideFileHandlerFactory(appCtrl: ApplicationController, archiveManager: ArchiveManager,
                                executionContextExecutor: ExecutionContextExecutor): FileHandlerFactory = {
    new FileHandlerFactory(appCtrl, archiveManager, executionContextExecutor)
  }

  @Provides
  @Singleton
  def provideFilesystemsManager(osResolver: OSResolver, config: Configuration): FilesystemsManager = {
    val fsMgr = new FilesystemsManager(osResolver, config)
    fsMgr.init()
    fsMgr
  }

  @Provides
  @Singleton
  def providePluginManager(filesystemsManager: FilesystemsManager,
                           archiveManager: ArchiveManager): PluginManager =
    new PluginManager(filesystemsManager, archiveManager)

  @Provides
  @Singleton
  def provideStatusManager(@Named("LeftPanel") leftDirTabMgr: DirTabManager,
                           @Named("RightPanel") rightDirTabMgr: DirTabManager,
                           eventBus: EventBus): StatusMgr = {
    new StatusMgr(leftDirTabMgr, rightDirTabMgr, eventBus)
  }

  @Provides
  @Singleton
  def provideOperationManager(statusMgr: StatusMgr,
                              appController: ApplicationController,
                              copyOperationCtrl: TransferOperationController,
                              mkDirOperationCtrl: MkDirOperationCtrl,
                              deleteOperationCtrl: DeleteOperationCtrl,
                              countDirStatsOperationCtrl: CountDirStatsOperationCtrl,
                              settingsCtrl: SettingsCtrl,
                              findFilesCtrl: FindFilesCtrl,
                              propertiesCtrl: PropertiesCtrl,
                              config: Configuration,
                              executionContextExecutor: ExecutionContextExecutor): OperationMgr = {
    new OperationMgr(statusMgr, appController, copyOperationCtrl, mkDirOperationCtrl, deleteOperationCtrl,
                     countDirStatsOperationCtrl, settingsCtrl, findFilesCtrl, propertiesCtrl, config,
                     executionContextExecutor)
  }

  @Provides
  @Singleton
  def provideBookmarkMgr(filesystemsManager: FilesystemsManager,
                         configuration: Configuration): BookmarkMgr = {
    val bookmarkMgr = new BookmarkMgr(configuration)
    bookmarkMgr.init(filesystemsManager)
    bookmarkMgr
  }

  @Provides
  @Singleton
  def provideHistoryMgr(eventBus: EventBus, fsManager: FilesystemsManager, config: Configuration): HistoryMgr = {
    val historyMgr = new HistoryMgr(config)
    historyMgr.init(fsManager)
    eventBus.register(historyMgr)
    historyMgr
  }

  @Provides
  @Singleton
  def provideEventBus() = new EventBus

  @Provides
  @Singleton
  def provideResourceManager() = new ResourceManager

  @Provides
  @Singleton
  def provideIconResolver(fileTypeManager: FileTypeManager, archiveManager: ArchiveManager, resourceMgr: ResourceManager) =
    new IconResolver(fileTypeManager, archiveManager, resourceMgr)

  @Provides
  @Singleton
  def provideCachedThreadPoolForRunningExternalApps(): ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  @Provides
  @Singleton
  def serviceRegistry() = new BackgroundServiceRegistry
}

class PanelContext(panelId: PanelId) extends PrivateModule with Logging {//with ScalaModule {

  override def configure(): Unit = {
    logger.trace(s"Configuring PanelContext for panelId=${panelId.toString}")
    bind(classOf[DirTabManager]).annotatedWith(Names.named(panelId.toString)).to(classOf[DirTabManager])
    expose(classOf[DirTabManager]).annotatedWith(Names.named(panelId.toString))
  }

  @Provides
  @Singleton
  def provideDirTabManager(eventBus: EventBus): DirTabManager = {
    new DirTabManager(panelId, eventBus)
  }
}

class UIOperationControllersContext extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  @Provides
  @Singleton
  def provideCopyOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                               resourceManager: ResourceManager, fsManager: FilesystemsManager,
                               userDecisionCtrl: UserDecisionCtrl,
                               configuration: Configuration,
                               serviceRegistry: BackgroundServiceRegistry) =
    new TransferOperationController(statusMgr, appController, resourceManager, fsManager,
                                    userDecisionCtrl, configuration, serviceRegistry)

  @Provides
  @Singleton
  def provideMkDirOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController) =
    new MkDirOperationCtrl(statusMgr, appController)

  @Provides
  @Singleton
  def provideDeleteOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                                 countDirOpCtrl: CountDirStatsOperationCtrl, userDecisionCtrl: UserDecisionCtrl,
                                 resourceMgr: ResourceManager, serviceRegistry: BackgroundServiceRegistry) =
    new DeleteOperationCtrl(statusMgr, appController, countDirOpCtrl,userDecisionCtrl, resourceMgr, serviceRegistry)

  @Provides
  @Singleton
  def provideCountDirStatsOperationCtrl(statusMgr: StatusMgr, appController: ApplicationController,
                                        resourceMgr: ResourceManager) =
    new CountDirStatsOperationCtrl(statusMgr, appController, resourceMgr)

  @Provides
  @Singleton
  def provideUserDecisionCtrl(resourceMgr: ResourceManager) =
    new UserDecisionCtrl(resourceMgr)

  @Provides
  @Singleton
  def provideSettingsCtrl(appController: ApplicationController) =
    new SettingsCtrl(appController)

  @Provides
  @Singleton
  def provideFindFilesCtrl(appController: ApplicationController, statusMgr: StatusMgr) =
    new FindFilesCtrl(appController, statusMgr)

  @Provides
  @Singleton
  def providePropertiesCtrl(statusMgr: StatusMgr, appController: ApplicationController) =
    new PropertiesCtrl(statusMgr, appController)
}

object ApplicationContext {
  lazy val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}