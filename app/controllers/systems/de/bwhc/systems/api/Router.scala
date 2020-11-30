package de.bwhc.systems.api



import javax.inject.Inject

import play.api.mvc.Results.{Ok,NotFound}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import play.api.libs.json.Json.toJson

import de.bwhc.rest.util.sapphyre.playjson._


class Router @Inject()(
  controller: SystemAgentController
)
extends SimpleRouter
{

  override def routes: Routes = {


    //-------------------------------------------------------------------------
    // Data Import                                               
    //-------------------------------------------------------------------------
    
    case GET(p"/")                           => controller.Action { Ok(toJson(SystemHypermedia.ApiResource)) }
    
    case GET(p"/schema/$rel")                => controller.Action {
                                                  SystemHypermedia.schemaFor(rel)
                                                    .map(toJson(_))
                                                    .map(Ok(_))
                                                    .getOrElse(NotFound(s"No JSON SChema availbale for Relation $rel"))
                                                }

    case POST(p"/data/upload")               => controller.processUpload
    case DELETE(p"/data/Patient/$id")        => controller.delete(id)


    //-------------------------------------------------------------------------
    // bwHC Node Peer-to-peer endpoints
    //-------------------------------------------------------------------------

    case POST(p"/peer2peer/query")           => controller.processPeerToPeerQuery
    case GET(p"/peer2peer/LocalQCReport")    => controller.getLocalQCReport

  }


}
