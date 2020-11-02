package de.bwhc.mtb.api



import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.syntax._

import play.api.libs.json.JsObject

import de.bwhc.mtb.data.entry.dtos.Patient
import de.bwhc.mtb.data.entry.api.DataQualityReport

import json._
import de.bwhc.util.json.schema._

trait Schemas
{

  implicit val patientWithStatusSchema = Json.schema[PatientWithStatus]
//  implicit val dataQualityReportSchema = Json.schema[DataQualityReport]

}
object Schemas extends Schemas



trait DataManagementHypermedia
{

  import de.bwhc.rest.util.cphl.Method._
  import de.bwhc.rest.util.cphl.Relations._

  import DataStatus._


  private val baseUrl = "/bwhc/mtb/api/data"


  private val GetPatientsWithStatus = Relation("PatientsWithStatus")
  private val GetPatientsForQC      = Relation("PatientsForQC")       
  private val GetMTBFile            = Relation("MTBFile")          
  private val GetDataQualityReport  = Relation("DataQualityReport")
  private val DeletePatient         = Delete       


  val apiActions =
    CPHL.empty[JsObject](
      Self                  -> Action(s"$baseUrl/"          , GET),
      GetPatientsWithStatus -> Action(s"$baseUrl/Patient"   , GET),
      GetPatientsForQC      -> Action(s"$baseUrl/qc/Patient", GET),
      DeletePatient         -> Action(s"$baseUrl/Patient/ID", DELETE),
    )


  implicit val hyperPatientWithStatus: PatientWithStatus => CPHL[PatientWithStatus] = {
    patient =>

      val Patient.Id(id) = patient.id 

      patient.status match {

        case CurationRequired =>         
          patient.withActions(
            GetMTBFile           -> Action(s"$baseUrl/MTBFile/$id"          , GET),
            GetDataQualityReport -> Action(s"$baseUrl/DataQualityReport/$id", GET),
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
        GetMTBFile           -> Action(s"$baseUrl/MTBFile/$id"          , GET),
        GetDataQualityReport -> Action(s"$baseUrl/DataQualityReport/$id", GET),
        DeletePatient        -> Action(s"$baseUrl/Patient/$id"          , DELETE)
      )
  }

}
object DataManagementHypermedia extends DataManagementHypermedia
