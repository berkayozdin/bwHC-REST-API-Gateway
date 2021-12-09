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
PlayBodyParsers,
  Request,
  Result
}
import play.api.libs.json.{
  Json, JsValue, Format
}

import de.bwhc.mtb.data.entry.dtos.{
  MTBFile,
  Patient,
  ZPM
}
//import de.bwhc.mtb.data.entry.dtos.Patient
import de.bwhc.mtb.data.entry.api.MTBDataService


import cats.data.{
  EitherT,
  OptionT
}
import cats.instances.future._
import cats.syntax.either._

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}

import de.bwhc.services.WrappedDataService

import de.bwhc.fhir.MTBFileBundle


class ETLController @Inject()(
  val controllerComponents: ControllerComponents,
  val dataService: WrappedDataService,
  override val parse: PlayBodyParsers
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

  private val FHIR_JSON = "application/fhir+json"


  //---------------------------------------------------------------------------
  // Data Import
  //---------------------------------------------------------------------------

/*
  def processUpload: Action[JsValue] =
    Action.async(parse.tolerantJson) { req =>

      import de.bwhc.fhir.Mappings._
      import org.hl7.fhir.r4.FHIRJson._

      val contentType = req.contentType

      val js = req.body

      val result =
        contentType.filter(_ == FHIR_JSON)
          .fold(
            js.validate[MTBFile]
          )(
            _ => js.asFHIR[MTBFileBundle].map(_.mapTo[MTBFile])
          )

      result.asEither
        .leftMap(Outcome.fromJsErrors(_))
        .leftMap(toJson(_))
        .fold(
          out => Future.successful(BadRequest(out)),
          mtbfile => {
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
                     Ok
//                     Ok(toJson(input))
           
                   case IssuesDetected(qc,_) => 
                     Created(toJson(qc))
           
                   case _ => InternalServerError
                 }
               )
             )
          }
        )
    }
*/

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

              case IssuesDetected(qc,_) => 
                Created(toJson(qc))

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
