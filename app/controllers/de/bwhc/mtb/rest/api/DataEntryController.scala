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



class DataEntryController @Inject()(
  val controllerComponents: ControllerComponents,
  val services: Services
)(
  implicit ec: ExecutionContext
)
extends RequestOps
{

  import de.bwhc.mtb.data.entry.dtos.{MTBFile,Patient}
  import de.bwhc.mtb.data.entry.api.MTBDataService
  import MTBDataService.Command._
  import MTBDataService.Response._


  private val service = services.dataService


  def processUpload: Action[AnyContent] =
    Action.async {

      implicit req =>

      processJson[MTBFile]{ mtbfile =>

        (service ! Upload(mtbfile))
          .map(
            _.leftMap(err => Outcome.fromErrors(List(err)))
             .leftMap(toJson(_))
             .fold(
               UnprocessableEntity(_),
               {
                 case Imported(issuesOrInput,_) =>
                   issuesOrInput
                     .bimap(toJson(_),toJson(_))
                     .fold(
                       Ok(_), 
                       Created(_).withHeaders(LOCATION -> s"/data/MTBFile/${mtbfile.patient.id.value}")
                     )
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
