package de.bwhc.mtb.api


import de.bwhc.mtb.data.entry.dtos._
import de.bwhc.mtb.data.entry.views.MolecularTherapyView
import de.bwhc.mtb.query.api._

import json.{Schema,Json}
import de.bwhc.util.json.schema._
import play.api.libs.json.{JsObject,JsValue}


object DTOSchemas
{

  implicit val icd10gmSchema = Json.schema[ICD10GM]("icd-10-code")
  implicit val geneSchema    = Json.schema[Variant.Gene]("gene-symbol")

//  implicit val medUsageSchema = Json.schema[Query.MedicationWithUsage]("medication-with-usage")
//  implicit val parametersSchema = Json.schema[Query.Parameters]("parameters")


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


