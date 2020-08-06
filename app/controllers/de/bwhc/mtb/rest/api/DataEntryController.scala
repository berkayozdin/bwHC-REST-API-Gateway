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



class DataEntryController @Inject()(
  val controllerComponents: ControllerComponents,
  val services: Services
)(
  implicit ec: ExecutionContext
)
extends RequestOps
{

  import MTBDataService.Command._
  import MTBDataService.Response._
  import MTBDataService.Error._


  private val service = services.dataService


  def processUpload: Action[AnyContent] =
    Action.async {

      implicit req =>

      processJson[MTBFile]{ mtbfile =>

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

   }


  def mtbfile(id: String): Action[AnyContent] =
    Action.async {
      toJsonOrElse(
        service mtbfile Patient.Id(id)
      )(
        s"Invalid Patient ID $id"
      )
    }


  def dataQualityReport(id: String): Action[AnyContent] =
    Action.async {
      toJsonOrElse(
        service dataQualityReport Patient.Id(id)
      )(
        s"Invalid Patient ID $id"
      )
    }



}
