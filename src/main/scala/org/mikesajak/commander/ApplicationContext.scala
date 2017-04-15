package org.mikesajak.commander

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.{Configuration, SimpleConfig}
import org.mikesajak.commander.fs.FsMgr

/**
  * Created by mike on 09.04.17.
  */
class ApplicationContext extends AbstractModule with ScalaModule {
  def configure() = {
    //    bind[ButtonBarController].to(classOf[ButtonBarController])
    //    bind[PanelsController].to(classOf[PanelsController])
    bind(classOf[ButtonBarController])
    bind(classOf[PanelsController])

    //    bind[Configuration].to(classOf[SimpleConfig])
    val configuration = new SimpleConfig
    bind[Configuration].toInstance(configuration)

    bind(classOf[ApplicationController])


    FsMgr.init()

  }
}
