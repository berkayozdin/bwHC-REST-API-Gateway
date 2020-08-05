package de.bwhc.mtb.rest.api



import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._


class Router @Inject()(
  examples: ExampleProvider,
  dataEntry: DataEntryController
)
extends SimpleRouter
{

  override def routes: Routes = {

    //-------------------------------------------------------------------------
    // Data example endpoints
    //-------------------------------------------------------------------------
    case GET(p"/data/examples/MTBFile")                  => examples.mtbfile


    //-------------------------------------------------------------------------
    // Data Management endpoints
    //-------------------------------------------------------------------------

    case POST(p"/data/MTBFile")                          => dataEntry.processUpload
    case GET(p"/data/MTBFile/$id")                       => dataEntry.mtbfile(id)

    //TODO: List of Patients with DataQualityReport
    
    case GET(p"/data/DataQualityReport/$id")             => dataEntry.dataQualityReport(id)


  }


}
