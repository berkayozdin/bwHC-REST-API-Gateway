package de.bwhc.mtb.api



import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.syntax._

import play.api.libs.json.JsObject

import de.bwhc.mtb.data.entry.dtos.Patient



trait DataManagementHypermedia
{

  import de.bwhc.rest.util.cphl.Method._
  import de.bwhc.rest.util.cphl.Relations._

  import DataStatus._

  private val baseUrl = "/bwhc/mtb/api/data"


  val apiCPHL =
    CPHL.empty[JsObject](
      Self                           -> Action(s"$baseUrl/"       , GET),
      Relation("PatientsWithStatus") -> Action(s"$baseUrl/Patient", GET),
      Relation("PatientsForQC")      -> Action(s"$baseUrl/qc/Patient", GET)
    )


  implicit val hyperPatientWithStatus: PatientWithStatus => CPHL[PatientWithStatus] = {
    patient =>

      val Patient.Id(id) = patient.id 

      patient.status match {

        case CurationRequired =>         
          patient.withActions(
            Relation("MTBFile")           -> Action(s"$baseUrl/MTBFile/$id"          , GET),
            Relation("DataQualityReport") -> Action(s"$baseUrl/DataQualityReport/$id", GET)
          )

        case ReadyForReporting =>
          CPHL(patient)
        
      }

  }
       

  implicit val hyperPatient: Patient => CPHL[Patient] = {
    patient =>

      val Patient.Id(id) = patient.id 

      patient.withActions(
        Relation("MTBFile")           -> Action(s"$baseUrl/MTBFile/$id"          , GET),
        Relation("DataQualityReport") -> Action(s"$baseUrl/DataQualityReport/$id", GET)
      )
  }

}
object DataManagementHypermedia extends DataManagementHypermedia
