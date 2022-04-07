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

import de.bwhc.mtb.data.entry.dtos.{
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

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}

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

  private val BWHC_SITE_ORIGIN  = "bwhc-site-origin"
  private val BWHC_QUERY_USERID = "bwhc-query-userid"


  def getLocalQCReport: Action[AnyContent] = 
    Action.async {

      request =>

      val result =
        for {
          form    <- request.body.asFormUrlEncoded
          origin  <- form.get(BWHC_SITE_ORIGIN).flatMap(_.headOption).map(ZPM(_))
          querier <- form.get(BWHC_QUERY_USERID).flatMap(_.headOption).map(Querier(_))
          res =
            for {
              qc      <- queryService.instance.getLocalQCReportFor(origin,querier)
              outcome =  qc.leftMap(List(_))
                           .leftMap(Outcome.fromErrors)
            } yield outcome.toJsonResult
        
        } yield res

      result.getOrElse(
        Future.successful(
          BadRequest(s"Missing Form Parameter(s): $BWHC_SITE_ORIGIN and/or $BWHC_QUERY_USERID")
        )
      )
    }
 

  def processQuery: Action[AnyContent] = 
    JsonAction[PeerToPeerQuery]{
      query =>
        queryService.instance.resultsOf(query)
          .map(SearchSet(_))
          .map(Json.toJson(_))
          .map(Ok(_))
    }


  def processMTBFileRequest: Action[AnyContent] = 
    JsonAction[PeerToPeerMTBFileRequest]{
      req =>
        queryService.instance.process(req)
          .map {
            case Some(snp) => Ok(Json.toJson(snp))
            case None      => NotFound
          }
    }

}
