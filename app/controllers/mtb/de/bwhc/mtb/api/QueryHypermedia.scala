package de.bwhc.mtb.api


import de.bwhc.mtb.query.api.Query
  
import de.bwhc.rest.util.hal._
import de.bwhc.rest.util.hal.syntax._
import de.bwhc.rest.util.hal.Relations._


trait QueryHypermedia
{

/*
  val Patients               = Relation("Patients")
  val NGSSummaries           = Relation("NGSSummaries")
  val TherapyRecommendations = Relation("TherapyRecommendations")
  val MolecularTherapies     = Relation("MolecularTherapies")
*/

  private val baseUrl = "/bwhc/mtb/api/query"


  implicit val hyperQuery: Query => Hyper[Query] = {
    query =>

      val queryId = query.id.value 

      query.withLinks(
        Self                               -> s"/$baseUrl/${queryId}",
        Relation("update")                 -> s"/$baseUrl/${queryId}",        //TODO: add HTTP Method
        Relation("filter")                 -> s"/$baseUrl/${queryId}/filter", //TODO: add HTTP Method
        Relation("Patients")               -> s"/$baseUrl/${queryId}/Patient",
        Relation("NGSSummaries")           -> s"/$baseUrl/${queryId}/NGSSummary",
        Relation("TherapyRecommendations") -> s"/$baseUrl/${queryId}/TherapyRecommendation",
        Relation("MolecularTherapies")     -> s"/$baseUrl/${queryId}/MolecularTherapy",
      )
  }


}


