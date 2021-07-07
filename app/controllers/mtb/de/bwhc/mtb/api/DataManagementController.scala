package de.bwhc.mtb.api



import java.time.{LocalDate,YearMonth}

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

import cats.Id
import cats.syntax.either._

import de.bwhc.mtb.data.entry.dtos.{
  Gender,
  HealthInsurance,
  MTBFile,
  Patient
}
import de.bwhc.mtb.data.entry.views.MTBFileView

import de.bwhc.mtb.data.entry.api.MTBDataService

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}

import de.bwhc.rest.util.cphl.syntax._

import de.bwhc.rest.util.sapphyre.Hyper
import de.bwhc.rest.util.sapphyre.playjson._

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
  birthDate: Option[YearMonth],
  insurance: Option[HealthInsurance.Id],
  dateOfDeath: Option[YearMonth],
  status: DataStatus.Value
)
object PatientWithStatus
{

  def apply(status: DataStatus.Value): Patient => PatientWithStatus = {
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

  import de.bwhc.util.json.time._

  implicit val format = Json.format[PatientWithStatus]

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
{

  import DataManagementPermissions._
  import DataQualityHypermedia._

  import MTBDataService.Command._
  import MTBDataService.Response._
  import MTBDataService.Error._


  implicit val authService = sessionManager.instance




  def patientsForQC: Action[AnyContent] =
    AuthenticatedAction( DataQualityAccessRights )
      .async {
        for {
          pats      <- dataService.instance.patientsWithIncompleteData 
          hyperPats =  HyperPatients(pats)
          json      =  toJson(hyperPats)   
        } yield Ok(json)
      }

/*
  def patientsForQC(
    genders: Seq[String],
    errorMsg: Option[String],
    entityType: Option[String],
    attribute: Option[String]
  ): Action[AnyContent] =
    AuthenticatedAction( DataQualityAccessRights )
      .async {

        val filter =
          MTBDataService.Filter(
            Option(genders.toSet.map(Gender.withName)).filterNot(_.isEmpty),
            errorMsg,
            entityType,
            attribute
          )

        for {
          pats      <- dataService.instance.patientsWithIncompleteData(filter) 
          hyperPats =  HyperPatients(pats)
          json      =  toJson(hyperPats)   
        } yield Ok(json)
      }
*/

  
  def mtbfile(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityAccessRights).async {

      dataService.instance
        .mtbfile(Patient.Id(id))
        .map(
          _.map(HyperMTBFile(_)) 
           .map(toJson(_))
           .map(Ok(_))
           .getOrElse(NotFound(s"Invalid Patient ID $id"))
        )

    }


  def mtbfileView(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityAccessRights).async {

      dataService.instance
        .mtbfileView(Patient.Id(id))
        .map(
          _.map(toJson(_))
           .map(Ok(_))
           .getOrElse(NotFound(s"Invalid Patient ID $id"))
        )

    }


  def dataQualityReport(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityAccessRights).async {

      dataService.instance
        .dataQualityReport(Patient.Id(id))
        .map(
          _.map(HyperDataQualityReport(_)) 
           .map(toJson(_))
           .map(Ok(_))
           .getOrElse(NotFound(s"Invalid Patient ID $id"))
        )

    }



  def delete(id: String): Action[AnyContent] =
    AuthenticatedAction( DataQualityAccessRights ).async {

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
