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
    //    bind[ButtonBarController].to(classOf[ButtonBarController])
    //    bind[PanelsController].to(classOf[PanelsController])
    bind(classOf[ButtonBarController]).in(classOf[Singleton])
    bind(classOf[PanelsController]).in(classOf[Singleton])

    //    bind[Configuration].to(classOf[SimpleConfig])
    val configuration = new TypesafeConfig(ApplicationController.configFile)
    bind[Configuration].toInstance(configuration)

    bind(classOf[ApplicationController]).in(classOf[Singleton])

    FsMgr.init()

  }
}

object ApplicationContext {
  val globalInjector: Injector = Guice.createInjector(new ApplicationContext)
}
