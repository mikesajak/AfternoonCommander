package org.mikesajak.commander

import com.google.inject.{AbstractModule, Guice, Injector, Singleton}
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.{Configuration, TypesafeConfig}
import org.mikesajak.commander.fs.FsMgr

/**
  * Created by mike on 09.04.17.
  */
class ApplicationContext extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    val configuration = new TypesafeConfig(ApplicationController.configFile)
    bind[Configuration].toInstance(configuration)

    bind(classOf[ApplicationController]).in(classOf[Singleton])

    val fileTypeManager = initFileTypeManager()
    bind[FileTypeManager].toInstance(fileTypeManager)

    val fsMgr = new FsMgr()
    fsMgr.init()
    bind[FsMgr].toInstance(fsMgr)
  }

  private def initFileTypeManager() = {
    val fileTypeManager = new FileTypeManager()

    fileTypeManager.registerIcon(DirectoryType, "ic_folder_black_24dp_1x.png")
    fileTypeManager.registerIcon(ParentDirectoryType, "ic_arrow_back_black_24dp_1x.png")
    fileTypeManager.registerIcon(GraphicFile, "ic_image_black_24dp_1x.png")


    fileTypeManager
  }
}

object ApplicationContext {
  val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}