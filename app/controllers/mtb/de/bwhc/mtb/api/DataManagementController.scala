package de.bwhc.mtb.api



import java.time.LocalDate

import scala.util.{
  Left,
  Right
}
import scala.concurrent.{
  Future,
  ExecutionContext
}

import javax.inject.Inject

import play.api.mvc.{
  Action,
  AnyContent,
  BodyParsers,
  BaseController,
  ControllerComponents,
  Request,
  Result
}
import play.api.libs.json.{Json,JsObject}
import Json.toJson

import cats.syntax.either._

import de.bwhc.mtb.data.entry.dtos.{
  Gender,
  HealthInsurance,
  MTBFile,
  Patient
}

import de.bwhc.mtb.data.entry.api.MTBDataService

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}

import de.bwhc.auth.core._
import de.bwhc.auth.api._


import de.bwhc.services._



object DataStatus extends Enumeration
{
  val CurationRequired  = Value
  val ReadyForReporting = Value

  implicit val format = Json.formatEnum(this)
}


final case class PatientWithStatus
(
  id: Patient.Id,
  gender: Gender.Value,
  birthDate: Option[LocalDate],
  insurance: Option[HealthInsurance.Id],
  dateOfDeath: Option[LocalDate],
  status: DataStatus.Value
)


import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.syntax._


object PatientWithStatus
{
  implicit val format = Json.format[PatientWithStatus]
}



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

/*
  implicit val hyperPatientWithStatus: PatientWithStatus => CPHL[PatientWithStatus] = {
    patient =>

      val Patient.Id(id) = patient.id 

      patient.status match {

        case CurationRequired =>         
          patient.withLinks(
            Relation("MTBFile")           -> Action(s"$baseUrl/MTBFile/$id"          , GET),
            Relation("DataQualityReport") -> Action(s"$baseUrl/DataQualityReport/$id", GET)
          )
        
        case ReadyForReporting =>
          patient.withLinks()
        
      }
  }
*/

  implicit val hyperPatient: Patient => CPHL[Patient] = {
    patient =>

      val Patient.Id(id) = patient.id 

      patient.withLinks(
        Relation("MTBFile")           -> Action(s"$baseUrl/MTBFile/$id"          , GET),
        Relation("DataQualityReport") -> Action(s"$baseUrl/DataQualityReport/$id", GET)
      )
  }

}




class DataManagementController @Inject()(
  val controllerComponents: ControllerComponents,
  val dataService: WrappedDataService,
  val queryService: WrappedQueryService,
  val sessionManager: WrappedSessionManager
)(
  implicit ec: ExecutionContext
)
extends BaseController
with RequestOps
with AuthenticationOps[UserWithRoles]
with DataManagementPermissions
with DataManagementHypermedia
{


  import MTBDataService.Command._
  import MTBDataService.Response._
  import MTBDataService.Error._

  implicit val authService = sessionManager.instance


  private def Status(status: DataStatus.Value): Patient => PatientWithStatus = {
    patient =>
      PatientWithStatus(
        patient.id, 
        patient.gender, 
        patient.birthDate, 
        patient.insurance, 
        patient.dateOfDeath, 
        status
      )
  }



  def apiHypermedia: Action[AnyContent] = 
    Action {
      Ok(Json.toJson(apiCPHL))
    }


  import DataStatus._
  import de.bwhc.util.mapping.syntax._

  def patientsWithStatus: Action[AnyContent] =
    AuthenticatedAction( PatientStatusAccessRights )
      .async {
        for {
          patsForQC       <- dataService.instance.patientsWithIncompleteData

          patIDsForQc     =  patsForQC.map(_.id).toList

          patsInReporting <- queryService.instance.patients
         
          allPats         = patsForQC.map(_.mapTo(Status(CurationRequired))) ++
                              patsInReporting.filter(!patIDsForQc.contains(_))
                                .map(_.mapTo(Status(ReadyForReporting)))

          set  =  SearchSet(allPats.map(_.withHypermedia))

          json =  toJson(set)   

        } yield Ok(json)
      }



  def patientsForQC: Action[AnyContent] =
    AuthenticatedAction( DataQualityAccessRights )
      .async {
        for {
          pats   <- dataService.instance.patientsWithIncompleteData 

          hyperPats = pats.map(_.withHypermedia)

          set  =  SearchSet(hyperPats)
//          set  =  SearchSet(ps)
          json =  toJson(set)   
        } yield Ok(json)
      }



  def mtbfile(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityAccessRights).async {

      dataService.instance.mtbfile(Patient.Id(id))
        .map(_ toJsonOrElse (s"Invalid Patient ID $id"))

    }


  def dataQualityReport(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityAccessRights).async {

      dataService.instance.dataQualityReport(Patient.Id(id))
        .map(_ toJsonOrElse (s"Invalid Patient ID $id"))

    }



  def delete(id: String): Action[AnyContent] =
    AuthenticatedAction( DataQualityAccessRights )
      .async {

      (dataService.instance ! Delete(Patient.Id(id)))
        .map(
          _.fold(
            {
              case UnspecificError(msg) =>
                BadRequest(toJson(Outcome.fromErrors(List(msg))))

              case _ => InternalServerError
            },
            {
              case Deleted(_,_) => Ok

              case _ => InternalServerError
            }
          )
        ) 

      }


}
