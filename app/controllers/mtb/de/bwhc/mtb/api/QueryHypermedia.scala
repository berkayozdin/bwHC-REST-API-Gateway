package de.bwhc.mtb.api


import de.bwhc.mtb.query.api.Query

trait QueryHypermedia
{

  import de.bwhc.rest.util.hal._
  import de.bwhc.rest.util.hal.syntax._
  import de.bwhc.rest.util.hal.Relations._


  val Patients               = Relation("Patients")
  val NGSSummaries           = Relation("NGSSummaries")
  val TherapyRecommendations = Relation("TherapyRecommendations")
  val MolecularTherapies     = Relation("MolecularTherapies")


  implicit val hyperQuery: Query => Hyper[Query] = {
    query =>

      val queryId = query.id.value 

      query.withLinks(
        Self                   -> s"/bwhc/mtb/api/query/${queryId}",
        Patients               -> s"/bwhc/mtb/api/query/${queryId}/Patient",
        NGSSummaries           -> s"/bwhc/mtb/api/query/${queryId}/NGSSummary",
        TherapyRecommendations -> s"/bwhc/mtb/api/query/${queryId}/TherapyRecommendation",
        MolecularTherapies     -> s"/bwhc/mtb/api/query/${queryId}/MolecularTherapy",
      )
  }


}


