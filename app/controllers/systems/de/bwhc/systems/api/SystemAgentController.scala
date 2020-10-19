package de.bwhc.systems.api



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
import de.bwhc.mtb.data.entry.api.MTBDataService
import de.bwhc.mtb.query.api._


import cats.data.{
  EitherT,
  OptionT
}
import cats.instances.future._
import cats.syntax.either._

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}

import de.bwhc.services.{WrappedDataService,WrappedQueryService}


class SystemAgentController @Inject()(
  val controllerComponents: ControllerComponents,
  val dataService: WrappedDataService,
  val queryService: WrappedQueryService
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

      (dataService.instance ! MTBDataService.Command.Upload(mtbfile))
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


  //---------------------------------------------------------------------------
  // Peer-to-peer operations
  //---------------------------------------------------------------------------

  def getLocalQCReport: Action[AnyContent] = 
    Action.async {

      request =>

      //TODO: get originating ZPM and Querier from request
      
      val querier = Querier("TODO")

      val origin  = ZPM("TODO")


      for {
        qc      <- queryService.instance.getLocalQCReportFor(origin,querier)
        outcome = qc.leftMap(List(_))
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
 
    }
 

  def processPeerToPeerQuery: Action[AnyContent] = 
    JsonAction[PeerToPeerQuery]{
      query =>
        queryService.instance.resultsOf(query)
          .map(SearchSet(_))
          .map(Json.toJson(_))
          .map(Ok(_))
    }

}
