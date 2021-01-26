package de.bwhc.mtb.api



import javax.inject.Inject

import play.api.mvc.Results.{Ok,NotFound}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import de.bwhc.mtb.query.api.Query

import play.api.libs.json.Json.toJson
import de.bwhc.rest.util.sapphyre.playjson._


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
    
    case GET(p"/data/schema/$rel")                 => dataController.Action {
                                                        DataMgmtSchemas.schemaFor(rel)
                                                          .map(Ok(_))
                                                          .getOrElse(NotFound)
                                                      }

    case DELETE(p"/data/Patient/$id")              => dataController.delete(id)

    case GET(p"/data-quality")                     => dataController.Action {
                                                        Ok(toJson(DataQualityHypermedia.ApiResource))
                                                      }

    case GET(p"/data-quality/patients")                         => dataController.patientsForQC
    case GET(p"/data-quality/patients/$id/mtbfile")             => dataController.mtbfile(id)
    case GET(p"/data-quality/patients/$id/mtbfileview")         => dataController.mtbfileView(id)
    case GET(p"/data-quality/patients/$id/data-quality-report") => dataController.dataQualityReport(id)

    //----------------------
    //TODO: Remove
    case GET(p"/data/qc/Patient")                  => dataController.patientsForQC
    case GET(p"/data/MTBFile/$id")                 => dataController.mtbfile(id)
    case GET(p"/data/DataQualityReport/$id")       => dataController.dataQualityReport(id)
    //----------------------


    //-------------------------------------------------------------------------
    // ZPM QC Reports                                                          
    //-------------------------------------------------------------------------
    case GET(p"/reporting")                        => queryController.ReportingApi
    case GET(p"/reporting/LocalQCReport")          => queryController.getLocalQCReport
    case GET(p"/reporting/GlobalQCReport")         => queryController.getGlobalQCReport

    case GET(p"/reporting/QCReport"?q"scope=$scope") => 
      scope match {
        case "local"  => queryController.getLocalQCReport
        case "global" => queryController.getGlobalQCReport
        case _        => queryController.Action {
                           NotFound(s"Invalid query parameter 'scope', expected { local, global }")
                         }
      }


    //-------------------------------------------------------------------------
    // MTBFile Queries                                                  
    //-------------------------------------------------------------------------
    case GET(p"/query")                            => queryController.QueryApi

    case GET(p"/query/schema/$rel")                => queryController.Action {
                                                        QueryHypermedia.schema(rel)
                                                          .map(Ok(_))
                                                          .getOrElse(NotFound)
                                                      }
 
    case POST(p"/query")                           => queryController.submit
    case POST(p"/query/$id")                       => queryController.update(Query.Id(id))
    case PUT(p"/query/$id/filter")                 => queryController.applyFilter(Query.Id(id))

                                                  
    case GET(p"/query/$id/patients")                => queryController.patientsFrom(Query.Id(id))
    case GET(p"/query/$id/therapy-recommendations") => queryController.therapyRecommendationsFrom(Query.Id(id))
    case GET(p"/query/$id/molecular-therapies")     => queryController.molecularTherapiesFrom(Query.Id(id))
    case GET(p"/query/$id/ngs-summaries")           => queryController.ngsSummariesFrom(Query.Id(id))
    case GET(p"/query/$id/mtbfiles/$patId")         => queryController.mtbfileFrom(Query.Id(id),patId)


    //----------------------
    //TODO: Remove
    case GET(p"/query/$id")                        => queryController.query(Query.Id(id))
    case GET(p"/query/$id/Patient")                => queryController.patientsFrom(Query.Id(id))
    case GET(p"/query/$id/TherapyRecommendation")  => queryController.therapyRecommendationsFrom(Query.Id(id))
    case GET(p"/query/$id/MolecularTherapy")       => queryController.molecularTherapiesFrom(Query.Id(id))
    case GET(p"/query/$id/NGSSummary")             => queryController.ngsSummariesFrom(Query.Id(id))
    case GET(p"/query/$id/MTBFile/$patId")         => queryController.mtbfileFrom(Query.Id(id),patId)
    //----------------------


  }


}
