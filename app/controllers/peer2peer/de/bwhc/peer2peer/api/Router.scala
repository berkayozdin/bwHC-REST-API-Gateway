package de.bwhc.peer2peer.api



import javax.inject.Inject

import play.api.mvc.Results.{Ok,NotFound}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import play.api.libs.json.Json.toJson

import de.bwhc.rest.util.sapphyre.playjson._


class Router @Inject()(
  controller: Peer2PeerAPIController
)
extends SimpleRouter
{

  override def routes: Routes = {

    //-------------------------------------------------------------------------
    // bwHC Node Peer-to-peer endpoints
    //-------------------------------------------------------------------------

    case POST(p"/query")           => controller.processQuery

    case POST(p"/LocalQCReport")   => controller.getLocalQCReport
//    case GET(p"/LocalQCReport")    => controller.getLocalQCReport

  }


}
