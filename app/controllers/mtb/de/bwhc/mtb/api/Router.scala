package de.bwhc.mtb.api



import javax.inject.Inject

import play.api.mvc.Results.Ok
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import de.bwhc.mtb.query.api.Query

import play.api.libs.json.Json.toJson



class Router @Inject()(
  dataController: DataManagementController,
  queryController: QueryController,
)
extends SimpleRouter
{

  override def routes: Routes = {


    //-------------------------------------------------------------------------
    // Data Management endpoints                                               
    //-------------------------------------------------------------------------
    case GET(p"/data/")                            => dataController.Action { Ok(toJson(DataManagementHypermedia.apiActions)) }

    case GET(p"/data/Patient")                     => dataController.patientsWithStatus
    case DELETE(p"/data/Patient/$id")              => dataController.delete(id)

                                                   
    case GET(p"/data/qc/Patient")                  => dataController.patientsForQC
//    case GET(p"/data/qc/MTBFile/$id")              => dataController.mtbfile(id)
//    case GET(p"/data/qc/DataQualityReport/$id")    => dataController.dataQualityReport(id)

    case GET(p"/data/MTBFile/$id")                 => dataController.mtbfile(id)
    case GET(p"/data/DataQualityReport/$id")       => dataController.dataQualityReport(id)
                                                  


    //-------------------------------------------------------------------------
    // ZPM QC Reports                                                          
    //-------------------------------------------------------------------------
    case GET(p"/reporting/LocalQCReport")          => queryController.getLocalQCReport
    case GET(p"/reporting/GlobalQCReport")         => queryController.getGlobalQCReport


    //-------------------------------------------------------------------------
    // MTBFile Queries                                                  
    //-------------------------------------------------------------------------
    case GET(p"/query")                            => queryController.Action { Ok(toJson(QueryHypermedia.apiActions)) }
 
    case POST(p"/query")                           => queryController.submit
    case POST(p"/query/$id")                       => queryController.update(Query.Id(id))
    case PUT(p"/query/$id/filter")                 => queryController.applyFilter(Query.Id(id))
                                                  
    case GET(p"/query/$id")                        => queryController.query(Query.Id(id))
    case GET(p"/query/$id/Patient")                => queryController.patientsFrom(Query.Id(id))
    case GET(p"/query/$id/MTBFile/$patId")         => queryController.mtbfileFrom(Query.Id(id),patId)
    case GET(p"/query/$id/TherapyRecommendation")  => queryController.therapyRecommendationsFrom(Query.Id(id))
    case GET(p"/query/$id/MolecularTherapy")       => queryController.molecularTherapiesFrom(Query.Id(id))
    case GET(p"/query/$id/NGSSummary")             => queryController.ngsSummariesFrom(Query.Id(id))


  }


}
