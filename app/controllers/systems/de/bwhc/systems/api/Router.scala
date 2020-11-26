package de.bwhc.systems.api



import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._


class Router @Inject()(
  controller: SystemAgentController
)
extends SimpleRouter
{

  override def routes: Routes = {


    //-------------------------------------------------------------------------
    // Data Import                                               
    //-------------------------------------------------------------------------

    case POST(p"/data/upload")                  => controller.processUpload
    case DELETE(p"/data/Patient/$id")           => controller.delete(id)


    //-------------------------------------------------------------------------
    // bwHC Node Peer-to-peer endpoints
    //-------------------------------------------------------------------------

    case POST(p"/peer2peer/query")              => controller.processPeerToPeerQuery
    case GET(p"/peer2peer/LocalQCReport")       => controller.getLocalQCReport

  }


}
