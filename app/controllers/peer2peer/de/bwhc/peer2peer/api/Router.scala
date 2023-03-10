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

    // Return Ok by default, as this implies this node is "online"
    case GET(p"/status")                     => controller.Action { Ok }
                                             
                                             
    case POST(p"/query")                     => controller.processQuery
                                             
    case POST(p"/LocalQCReport")             => controller.getLocalQCReport

    case POST(p"/medication-distribution")   => controller.getMedicationDistributionReport
    
    case POST(p"/tumor-entity-distribution") => controller.getTumorEntityDistributionReport

    case POST(p"/patient-therapies")         => controller.getPatientTherapies

    case POST(p"/MTBFile:request")           => controller.processMTBFileRequest

  }


}
