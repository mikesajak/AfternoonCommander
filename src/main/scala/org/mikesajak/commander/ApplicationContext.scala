package org.mikesajak.commander

import com.google.inject._
import com.google.inject.name.Names
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.{Configuration, TypesafeConfig}
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}
import org.mikesajak.commander.ui.controller.{DirTabManager, PanelId}

/**
  * Created by mike on 09.04.17.
  */
class ApplicationContext extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    install(new PanelContext(LeftPanel))
    install(new PanelContext(RightPanel))
  }

  @Provides
  @Singleton
  def provideConfig(): Configuration = {
    val config = new TypesafeConfig(ApplicationController.configFile)
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

//  @Provides
//  @Singleton
//  def provideDirTabManager(): DirTabManager = new DirTabManager()
}

class PanelContext(panelId: PanelId) extends PrivateModule {//with ScalaModule {
  println(s"Creating PanelContext with panelId=$panelId")
  override def configure(): Unit = {
    println(s"Configuring PanelContext for panelId=${panelId.toString}")
    bind(classOf[DirTabManager]).annotatedWith(Names.named(panelId.toString))
      //.to(classOf[DirTabManager])
      .toInstance(new DirTabManager(panelId)) // todo: use better way - some provider etc...
    expose(classOf[DirTabManager]).annotatedWith(Names.named(panelId.toString))
  }

  @Provides
  @Singleton
  def provideDirTabManager(): DirTabManager = new DirTabManager(panelId)
}


object ApplicationContext {
  val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}