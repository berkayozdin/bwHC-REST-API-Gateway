package de.bwhc.etl.api



import scala.concurrent.{
  Future,
  ExecutionContext
}

import javax.inject.Inject

import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  ControllerComponents,
  Request,
  Result
}
import play.api.libs.json.{
  Json, Format
}

import de.bwhc.mtb.data.entry.dtos.{
  MTBFile,
  Patient,
  ZPM
}
import de.bwhc.mtb.data.entry.dtos.Patient
import de.bwhc.mtb.data.entry.api.MTBDataService


import cats.data.{
  EitherT,
  OptionT
}
import cats.instances.future._
import cats.syntax.either._

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}

import de.bwhc.services.WrappedDataService


class SystemAgentController @Inject()(
  val controllerComponents: ControllerComponents,
  val dataService: WrappedDataService
)(
  implicit ec: ExecutionContext
)
extends BaseController
with RequestOps
{


  import Json.toJson

  import MTBDataService.Command._
  import MTBDataService.Response._
  import MTBDataService.Error._


  //---------------------------------------------------------------------------
  // Data Import
  //---------------------------------------------------------------------------

  def processUpload: Action[AnyContent] =
    JsonAction[MTBFile]{ mtbfile =>

      (dataService.instance ! Upload(mtbfile))
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
//                  .withHeaders(LOCATION -> s"/data/MTBFile/${mtbfile.patient.id.value}")

              case IssuesDetected(qc,_) => 
                Created(toJson(qc))
//                  .withHeaders(LOCATION -> s"/data/DataQualityReport/${mtbfile.patient.id.value}")

              case _ => InternalServerError
            }
          )
        )
    }


  def delete(patId: String): Action[AnyContent] = {
     Action.async {
       (dataService.instance ! Delete(Patient.Id(patId)))
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


}
