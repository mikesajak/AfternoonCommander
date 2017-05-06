package org.mikesajak.commander

import com.google.inject._
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.{Configuration, TypesafeConfig}
import org.mikesajak.commander.fs.FilesystemsManager

/**
  * Created by mike on 09.04.17.
  */
class ApplicationContext extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    requestInjection(this)
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

}

object ApplicationContext {
  val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}