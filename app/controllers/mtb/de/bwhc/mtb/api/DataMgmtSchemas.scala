package de.bwhc.mtb.api



import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.syntax._

import play.api.libs.json.{JsObject, JsValue}

import de.bwhc.mtb.dtos.{Patient,MTBFile}
import de.bwhc.mtb.data.entry.api.DataQualityReport

import json._
import com.github.andyglow.jsonschema.CatsSupport._

import de.bwhc.util.json.schema._


trait DataMgmtSchemas
{

  implicit val patientSchema           = Json.schema[Patient]
  implicit val patientWithStatusSchema = Json.schema[PatientWithStatus]

  implicit val dataQualityIssueSchema  = Json.schema[DataQualityReport.Issue]("DataQualityIssue")
  implicit val dataQualityReportSchema = Json.schema[DataQualityReport]

  implicit val mtbFileSchema           = Json.schema[MTBFile]


  private val GetPatientsWithStatus = Relation("get-all-patients-with-status")
  private val GetPatientsForQC      = Relation("get-all-patients-for-qc")       
  private val GetMTBFile            = Relation("get-mtbfile")
  private val GetDataQualityReport  = Relation("get-data-quality-report")
  private val DeletePatient         = Relation("delete")


  private val schemaMap =
    Map(
      GetPatientsWithStatus   -> JsValueSchema[PatientWithStatus],
      GetPatientsForQC        -> JsValueSchema[Patient],
      GetMTBFile              -> JsValueSchema[MTBFile],
      GetDataQualityReport    -> JsValueSchema[DataQualityReport]    
    )

  def schemaFor(rel: String): Option[JsValue] =
    schemaMap.get(Relation(rel))

}
object DataMgmtSchemas extends DataMgmtSchemas

