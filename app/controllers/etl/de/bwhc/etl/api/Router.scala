package de.bwhc.etl.api



import javax.inject.Inject

import play.api.mvc.Results.{Ok,NotFound}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import play.api.libs.json.Json.toJson

import de.bwhc.rest.util.sapphyre.playjson._


class Router @Inject()(
  controller: ETLController
)
extends SimpleRouter
{

  override def routes: Routes = {


    //-------------------------------------------------------------------------
    // Data Import                                               
    //-------------------------------------------------------------------------
    
    case GET(p"/")                           => controller.Action { Ok(toJson(ETLHypermedia.ApiResource)) }
    
    case GET(p"/schema/$rel")                => controller.Action {
                                                  ETLHypermedia.schemaFor(rel)
                                                    .map(toJson(_))
                                                    .map(Ok(_))
                                                    .getOrElse(NotFound(s"No JSON Schema available for Relation $rel"))
                                                }

    case POST(p"/data/upload")               => controller.processUpload
    case DELETE(p"/data/Patient/$id")        => controller.delete(id)

    case POST(p"/MTBFile")                   => controller.processUpload
    case DELETE(p"/MTBFile/$id")             => controller.delete(id)
    case DELETE(p"/Patient/$id")             => controller.delete(id)

  }


}
