package de.bwhc.fake.data



import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._



class Router @Inject()(
  examples: ExampleProvider
)
extends SimpleRouter
{

  override def routes: Routes = {

    //-------------------------------------------------------------------------
    // Data example endpoints
    //-------------------------------------------------------------------------
    case GET(p"/MTBFile")         => examples.mtbfile


  }


}
