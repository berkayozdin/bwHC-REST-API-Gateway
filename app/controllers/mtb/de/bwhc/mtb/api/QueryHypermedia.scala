package de.bwhc.mtb.api


import de.bwhc.mtb.data.entry.dtos._
import de.bwhc.mtb.query.api._
  
import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.syntax._
import de.bwhc.rest.util.cphl.Method._
import de.bwhc.rest.util.cphl.Relations._
import de.bwhc.rest.util.cphl.Action.Format
import de.bwhc.rest.util.cphl.Action.Format._

import json.{Schema,Json}
import de.bwhc.util.json.schema._
import play.api.libs.json.{JsObject,JsValue}


object DTOSchemas
{

  implicit val icd10gmSchema = Json.schema[ICD10GM]("icd-10-code")
  implicit val geneSchema    = Json.schema[Variant.Gene]("gene-symbol")


  implicit def codingSystemSchema[T](
    implicit system: Coding.System[T]
  ): Schema[Coding.System[T]] =
    const(system)


  import Schema.`object`.Field

  implicit def codingSchema[T](
    implicit
    codeSch: Schema[T],
    systemSch: Schema[Coding.System[T]]
  ): Schema[Coding[T]] = {
    val sch =
      Schema.`object`(
        Field("code", codeSch),
        Field("display", Json.schema[String], false),
        Field("system", systemSch),
        Field("version", Json.schema[String], false)
      )
    sch(s"coding-${codeSch.refName.get}")
  }

}


import QueryOps.Command


trait QuerySchemas
{

  import de.bwhc.util.json.schema.workarounds._
  import DTOSchemas._

  implicit val querySchema       = Json.schema[Query]
  implicit val queryFormSchema   = Json.schema[QueryForm]
  implicit val queryUpdateSchema = Json.schema[Command.Update]
  implicit val queryFilterSchema = Json.schema[Command.ApplyFilter]
  implicit val patientViewSchema = Json.schema[PatientView]
  implicit val ngsSummarySchema  = Json.schema[NGSSummary]
  implicit val thRecommSchema    = Json.schema[TherapyRecommendation]
  implicit val molThViewSchema   = Json.schema[MolecularTherapyView]

}
object QuerySchemas extends QuerySchemas



trait QueryHypermedia
{

  import QuerySchemas._


  private val baseUrl = "/bwhc/mtb/api/query"

  val SubmitQuery               = Relation("submit-query")
  val UpdateQuery               = Relation("update-query")
  val ApplyFilter               = Relation("filter-query")
  val GetPatients               = Relation("get-patients-from-query")
  val GetNGSSummaries           = Relation("get-ngs-summaries-from-query")
  val GetTherapyRecommendations = Relation("get-therapy-recommendations-from-query")
  val GetMolecularTherapies     = Relation("get-molecular-therapies-from-query")


  private val schemaMap =
    Map(
      SubmitQuery               -> JsValueSchema[QueryForm],
      UpdateQuery               -> JsValueSchema[QueryOps.Command.Update],
      ApplyFilter               -> JsValueSchema[QueryOps.Command.ApplyFilter],
      GetPatients               -> JsValueSchema[PatientView],
      GetNGSSummaries           -> JsValueSchema[NGSSummary],
      GetTherapyRecommendations -> JsValueSchema[TherapyRecommendation],
      GetMolecularTherapies     -> JsValueSchema[MolecularTherapyView] 
    )


  def schemaFor(rel: String): Option[JsValue] =
    schemaMap.get(Relation(rel)) 

/*
  private val QueryIdPlaceholder = "QUERY_ID"


  implicit val apiActions =
    CPHL(
      Base                      -> Action(s"$baseUrl/" , GET),
      SubmitQuery               -> Action(s"$baseUrl"  , POST)
                                     .withFormats(
                                       JSON -> Format("application/json",s"$baseUrl/schema/${SubmitQuery.name}") 
                                     ),
      UpdateQuery               -> Action(s"$baseUrl/$QueryIdPlaceholder"                      , POST)        
                                     .withFormats(
                                       JSON -> Format("application/json",s"$baseUrl/schema/${UpdateQuery.name}") 
                                     ),
      ApplyFilter               -> Action(s"$baseUrl/$QueryIdPlaceholder/filter"               , POST) 
                                     .withFormats(
                                       JSON -> Format("application/json",s"$baseUrl/schema/${ApplyFilter.name}") 
                                     ),
      GetPatients               -> Action(s"$baseUrl/$QueryIdPlaceholder/Patient"              , GET)
                                     .withFormats(
                                       JSON -> Format("application/json",s"$baseUrl/schema/${GetPatients.name}") 
                                     ),
      GetNGSSummaries           -> Action(s"$baseUrl/$QueryIdPlaceholder/NGSSummary"           , GET)
                                     .withFormats(
                                       JSON -> Format("application/json",s"$baseUrl/schema/${GetNGSSummaries.name}") 
                                     ),
      GetTherapyRecommendations -> Action(s"$baseUrl/$QueryIdPlaceholder/TherapyRecommendation", GET)
                                     .withFormats(
                                       JSON -> Format("application/json",s"$baseUrl/schema/${GetTherapyRecommendations.name}") 
                                     ),
      GetMolecularTherapies     -> Action(s"$baseUrl/$QueryIdPlaceholder/MolecularTherapy"     , GET)
                                     .withFormats(
                                       JSON -> Format("application/json",s"$baseUrl/schema/${GetMolecularTherapies.name}") 
                                     )
    )
  

  implicit val hyperQuery: Query => CPHL[Query] = {
    query =>

      val queryId = query.id.value 

      query.withActions(
        Self                      -> Action(s"$baseUrl/$queryId"                      , GET),
        UpdateQuery               -> Action(s"$baseUrl/$queryId"                      , POST)        
                                      .withFormats(
                                        JSON -> Format("application/json",s"$baseUrl/schema/${UpdateQuery.name}") 
                                      ),
        ApplyFilter               -> Action(s"$baseUrl/$queryId/filter"               , POST) 
                                       .withFormats(
                                         JSON -> Format("application/json",s"$baseUrl/schema/${ApplyFilter.name}") 
                                       ),
        GetPatients               -> Action(s"$baseUrl/$queryId/Patient"              , GET)
                                       .withFormats(
                                         JSON -> Format("application/json",s"$baseUrl/schema/${GetPatients.name}") 
                                       ),
        GetNGSSummaries           -> Action(s"$baseUrl/$queryId/NGSSummary"           , GET)
                                       .withFormats(
                                         JSON -> Format("application/json",s"$baseUrl/schema/${GetNGSSummaries.name}") 
                                       ),
        GetTherapyRecommendations -> Action(s"$baseUrl/$queryId/TherapyRecommendation", GET)
                                       .withFormats(
                                         JSON -> Format("application/json",s"$baseUrl/schema/${GetTherapyRecommendations.name}") 
                                       ),
        GetMolecularTherapies     -> Action(s"$baseUrl/$queryId/MolecularTherapy"     , GET)
                                       .withFormats(
                                         JSON -> Format("application/json",s"$baseUrl/schema/${GetMolecularTherapies.name}") 
                                       )
      )
  }
*/

}
object QueryHypermedia extends QueryHypermedia

