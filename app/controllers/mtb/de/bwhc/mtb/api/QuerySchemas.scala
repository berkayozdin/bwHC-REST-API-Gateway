package de.bwhc.mtb.api


import de.bwhc.mtb.data.entry.dtos._
import de.bwhc.mtb.data.entry.views.MolecularTherapyView
import de.bwhc.mtb.query.api._

import json.{Schema,Json}
import de.bwhc.util.data.{Interval,ClosedInterval}
import de.bwhc.util.json.schema._
import play.api.libs.json.{JsObject,JsValue}


object DTOSchemas
{

  implicit val icd10gmSchema = Json.schema[ICD10GM]("icd-10-code")
  implicit val geneSchema    = Json.schema[Gene.HgncId]("hgnc-id")


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


//  implicit def intervalSchema[T: Numeric: Schema]: Schema[Interval[T]] =
//    Json.schema[ClosedInterval[T]].asInstanceOf[Schema[Interval[T]]]

  implicit val intIntervalSchema: Schema[Interval[Int]] =
    Json.schema[ClosedInterval[Int]].asInstanceOf[Schema[Interval[Int]]]

  implicit val doubleIntervalSchema: Schema[Interval[Double]] =
    Json.schema[ClosedInterval[Double]].asInstanceOf[Schema[Interval[Double]]]

}

trait QuerySchemas
{
  import QueryOps.Command

  import de.bwhc.util.json.schema.workarounds._
  import DTOSchemas._

  implicit val querySchema       = Json.schema[Query]
  implicit val querySubmitSchema = Json.schema[Command.Submit]
  implicit val queryUpdateSchema = Json.schema[Command.Update]
  implicit val queryFilterSchema = Json.schema[Command.ApplyFilters]
//  implicit val queryFilterSchema = Json.schema[Command.ApplyFilter]
  implicit val patientViewSchema = Json.schema[PatientView]
  implicit val ngsSummarySchema  = Json.schema[NGSSummary]
  implicit val thRecommSchema    = Json.schema[TherapyRecommendation]

}
object QuerySchemas extends QuerySchemas


