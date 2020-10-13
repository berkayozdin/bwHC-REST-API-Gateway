package de.bwhc.mtb.rest.api



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
import play.api.libs.json.Json.toJson

import cats.syntax.either._

import de.bwhc.mtb.data.entry.dtos.{
  MTBFile,
  Patient
}
import de.bwhc.mtb.data.entry.api.MTBDataService

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}

import de.bwhc.auth.core._
import de.bwhc.auth.api._
import de.bwhc.rest.auth.WrappedSessionManager




class DataEntryController @Inject()(
  val controllerComponents: ControllerComponents,
  val dataService: WrappedDataService,
  val sessionManager: WrappedSessionManager
)(
  implicit ec: ExecutionContext
)
extends BaseController
with RequestOps
with AuthenticationOps[UserWithRoles]
{

  import Authorizations._

  import MTBDataService.Command._
  import MTBDataService.Response._
  import MTBDataService.Error._

  implicit val authService = sessionManager.instance

  private val service = dataService.instance


  def processUpload: Action[AnyContent] =
    JsonAction[MTBFile]{ mtbfile =>

      (service ! Upload(mtbfile))
        .map(
          _.fold(
            {
              case InvalidData(qc) =>
                UnprocessableEntity(toJson(qc))

              case UnspecificError(msg) =>
                BadRequest(toJson(Outcome.fromErrors(List(msg))))
            },
            {
              case Imported(input,_) =>
                Ok(toJson(input))
                  .withHeaders(LOCATION -> s"/data/MTBFile/${mtbfile.patient.id.value}")

              case IssuesDetected(qc,_) => 
                Created(toJson(qc))
                  .withHeaders(LOCATION -> s"/data/DataQualityReport/${mtbfile.patient.id.value}")

              case _ => InternalServerError
            }
          )
        )
    }


  def patients: Action[AnyContent] =
    AuthenticatedAction(DataQualityModeAccess).async {
      for {
        ps   <- service.patientsWithIncompleteData 
        set  =  SearchSet(ps)
        json =  toJson(set)   
      } yield Ok(json)
    }



  def mtbfile(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityModeAccess).async {

      service.mtbfile(Patient.Id(id))
        .map(_ toJsonOrElse (s"Invalid Patient ID $id"))

    }


  def dataQualityReport(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityModeAccess).async {

      service.dataQualityReport(Patient.Id(id))
        .map(_ toJsonOrElse (s"Invalid Patient ID $id"))

    }


  def delete(id: String): Action[AnyContent] =
    AuthenticatedAction(DataQualityModeAccess).async {

      (service ! Delete(Patient.Id(id)))
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
