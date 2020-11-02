package de.bwhc.mtb.api


import de.bwhc.mtb.query.api.{
  Query, QueryOps
}
  
import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.syntax._
import de.bwhc.rest.util.cphl.Method._
import de.bwhc.rest.util.cphl.Relations._

import json._
import de.bwhc.util.json.schema._

import play.api.libs.json.JsObject


trait QuerySchemas
{

  import QueryOps.Command

  implicit val querySchema       = Json.schema[Query]
  implicit val queryFormSchema   = Json.schema[QueryForm]
  implicit val queryFilterSchema = Json.schema[Command.ApplyFilter]

}
object QuerySchemas extends QuerySchemas


trait QueryHypermedia
{

  private val baseUrl = "/bwhc/mtb/api/query"

  val GetPatients               = Relation("patients")
  val GetNGSSummaries           = Relation("ngs-summaries")
  val GetTherapyRecommendations = Relation("therapy-recommendations")
  val GetMolecularTherapies     = Relation("molecular-therapies")
  val ApplyFilter               = Relation("apply-filter")


  implicit val apiActions =
    CPHL.empty[Query](
      Self                      -> Action(s"$baseUrl/"                        , GET),
      Create                    -> Action(s"$baseUrl/"                        , POST),        
      Update                    -> Action(s"$baseUrl/ID"                      , POST),        
      ApplyFilter               -> Action(s"$baseUrl/ID/filter"               , POST), 
      GetPatients               -> Action(s"$baseUrl/ID/Patient"              , GET),
      GetNGSSummaries           -> Action(s"$baseUrl/ID/NGSSummary"           , GET),
      GetTherapyRecommendations -> Action(s"$baseUrl/ID/TherapyRecommendation", GET),
      GetMolecularTherapies     -> Action(s"$baseUrl/ID/MolecularTherapy"     , GET)
    )
  

  implicit val hyperQuery: Query => CPHL[Query] = {
    query =>

      val queryId = query.id.value 

      query.withActions(
        Self                      -> Action(s"$baseUrl/$queryId"                      , GET),
        Update                    -> Action(s"$baseUrl/$queryId"                      , POST),        
        ApplyFilter               -> Action(s"$baseUrl/$queryId/filter"               , POST), 
        GetPatients               -> Action(s"$baseUrl/$queryId/Patient"              , GET),
        GetNGSSummaries           -> Action(s"$baseUrl/$queryId/NGSSummary"           , GET),
        GetTherapyRecommendations -> Action(s"$baseUrl/$queryId/TherapyRecommendation", GET),
        GetMolecularTherapies     -> Action(s"$baseUrl/$queryId/MolecularTherapy"     , GET)
      )
  }


}
object QueryHypermedia extends QueryHypermedia

