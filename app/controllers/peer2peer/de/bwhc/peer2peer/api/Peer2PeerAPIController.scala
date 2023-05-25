package de.bwhc.peer2peer.api



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
import de.bwhc.mtb.dtos.{
  MTBFile,
  Patient,
  ZPM
}
import de.bwhc.mtb.query.api._
import cats.data.{
  EitherT,
  OptionT
}
import cats.instances.future._
import cats.syntax.either._
import de.bwhc.rest.util.{
  Outcome,
  RequestOps,
  SearchSet
}
import de.bwhc.services.WrappedQueryService


class Peer2PeerAPIController @Inject()(
  val controllerComponents: ControllerComponents,
  val queryService: WrappedQueryService
)(
  implicit ec: ExecutionContext
)
extends BaseController
with RequestOps
{

  import Json.toJson


  //---------------------------------------------------------------------------
  // Peer-to-peer operations
  //---------------------------------------------------------------------------

  def getLocalQCReport = 
    JsonAction[PeerToPeerRequest[Map[String,String]]]{
      p2pReq =>
        queryService.instance.getLocalQCReport(p2pReq)
          .map(
            _.leftMap(List(_))
             .leftMap(Outcome.fromErrors)
             .toJsonResult
           )
    }
       

  def getMedicationDistributionReport =
    JsonAction[PeerToPeerRequest[Report.Filters]]{
      p2pReq =>
        queryService.instance
          .compileLocalMedicationDistributionFor(p2pReq)
          .map(
            _.leftMap(_.toList)
             .leftMap(Outcome.fromErrors)
             .toJsonResult
           )

    }


  def getTumorEntityDistributionReport =
    JsonAction[PeerToPeerRequest[Report.Filters]]{
      p2pReq =>
        queryService.instance
          .compileLocalTumorEntityDistributionFor(p2pReq)
          .map(
            _.leftMap(_.toList)
             .leftMap(Outcome.fromErrors)
             .toJsonResult
           )

    }


  def getPatientTherapies =
    JsonAction[PeerToPeerRequest[Report.Filters]]{
      p2pReq =>
        queryService.instance
          .compilePatientTherapies(p2pReq)
          .map(
            _.leftMap(_.toList)
             .leftMap(Outcome.fromErrors)
             .toJsonResult
           )

    }


  def processQuery =
    JsonAction[PeerToPeerRequest[Query.Parameters]]{
      query =>
        queryService.instance.resultsOf(query)
          .map(SearchSet(_))
          .map(Json.toJson(_))
          .map(Ok(_))
    }


  def processMTBFileRequest =
    JsonAction[PeerToPeerRequest[MTBFileParameters]]{
      req =>
        queryService.instance.process(req)
          .map {
            case Some(snp) => Ok(Json.toJson(snp))
            case None      => NotFound
          }
    }

}
