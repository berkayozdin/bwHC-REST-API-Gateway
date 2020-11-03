package de.bwhc.mtb.api



import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.syntax._

import play.api.libs.json.{JsObject, JsValue}

import de.bwhc.mtb.data.entry.dtos.{Patient,MTBFile}
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
}
object DataMgmtSchemas extends DataMgmtSchemas



trait DataManagementHypermedia
{

  import de.bwhc.rest.util.cphl.Method._
  import de.bwhc.rest.util.cphl.Relations._
  import de.bwhc.rest.util.cphl.Action.Format
  import de.bwhc.rest.util.cphl.Action.Format._

  import DataStatus._

  import DataMgmtSchemas._


  private val baseUrl = "/bwhc/mtb/api/data"


  private val GetPatientsWithStatus = Relation("get-all-patients-with-status")
  private val GetPatientsForQC      = Relation("get-all-patients-for-qc")       
  private val GetMTBFile            = Relation("get-mtbfile")
  private val GetDataQualityReport  = Relation("get-data-quality-report")
  private val DeletePatient         = Delete       


  private val schemaMap =
    Map(
      GetPatientsWithStatus   -> JsValueSchema[PatientWithStatus],
      GetPatientsForQC        -> JsValueSchema[Patient],
      GetMTBFile              -> JsValueSchema[MTBFile],
      GetDataQualityReport    -> JsValueSchema[DataQualityReport]    
    )

  def schemaFor(rel: String): Option[JsValue] =
    schemaMap.get(Relation(rel))



  val apiActions =
    CPHL.empty[JsObject](
      Self                  -> Action(s"$baseUrl/"          , GET),
      GetPatientsWithStatus -> Action(s"$baseUrl/Patient"   , GET)
                                 .withFormats(
                                   JSON -> Format("application/json",s"$baseUrl/schema/${GetPatientsWithStatus.name}")
                                 ),
      GetPatientsForQC      -> Action(s"$baseUrl/qc/Patient", GET)
                                 .withFormats(
                                   JSON -> Format("application/json",s"$baseUrl/schema/${GetPatientsForQC.name}")
                                 ),
      GetMTBFile            -> Action(s"$baseUrl/MTBFile/PATIENT_ID", GET)
                                 .withFormats(
                                   JSON -> Format("application/json",s"$baseUrl/schema/${GetMTBFile.name}")
                                 ),
      GetDataQualityReport  -> Action(s"$baseUrl/DataQualityReport/PATIENT_ID", GET)
                                 .withFormats(
                                   JSON -> Format("application/json",s"$baseUrl/schema/${GetDataQualityReport.name}")
                                 ),
      DeletePatient         -> Action(s"$baseUrl/Patient/PATIENT_ID", DELETE),
    )


  implicit val hyperPatientWithStatus: PatientWithStatus => CPHL[PatientWithStatus] = {
    patient =>

      val Patient.Id(id) = patient.id 

      patient.status match {

        case CurationRequired =>         
          patient.withActions(
            GetMTBFile           -> Action(s"$baseUrl/MTBFile/$id"          , GET)
                                      .withFormats(
                                        JSON -> Format("application/json",s"$baseUrl/schema/${GetMTBFile.name}")
                                      ),
            GetDataQualityReport -> Action(s"$baseUrl/DataQualityReport/$id", GET)
                                      .withFormats(
                                        JSON -> Format("application/json",s"$baseUrl/schema/${GetDataQualityReport.name}")
                                      ),
            DeletePatient        -> Action(s"$baseUrl/Patient/$id"          , DELETE)
          )

        case ReadyForReporting =>
          CPHL(patient)
        
      }
  }
       

  implicit val hyperPatient: Patient => CPHL[Patient] = {
    patient =>

      val Patient.Id(id) = patient.id 

      patient.withActions(
        GetMTBFile           -> Action(s"$baseUrl/MTBFile/$id"          , GET)
                                  .withFormats(
                                    JSON -> Format("application/json",s"$baseUrl/schema/${GetMTBFile.name}")
                                  ),
        GetDataQualityReport -> Action(s"$baseUrl/DataQualityReport/$id", GET)
                                  .withFormats(
                                    JSON -> Format("application/json",s"$baseUrl/schema/${GetDataQualityReport.name}")
                                  ),
        DeletePatient        -> Action(s"$baseUrl/Patient/$id"          , DELETE)
      )
  }

}
object DataManagementHypermedia extends DataManagementHypermedia
