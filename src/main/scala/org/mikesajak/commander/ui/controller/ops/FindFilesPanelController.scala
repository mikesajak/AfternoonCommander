package org.mikesajak.commander.ui.controller.ops

import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

trait FindFilesPanelController {

}

@sfxml
class FindFilesPanelControllerImpl(headerImageView: ImageView,
                               resourceMgr: ResourceManager)
    extends FindFilesPanelController {
  headerImageView.image = resourceMgr.getIcon("file-find.png", IconSize.Big)

}
