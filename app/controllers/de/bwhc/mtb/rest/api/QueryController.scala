package de.bwhc.mtb.rest.api



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



final case class QueryForm(
  mode: Query.Mode.Value,
  parameters: Query.Parameters
)

object QueryForm
{
  implicit val format = Json.format[QueryForm]
}




class QueryController @Inject()(
  val controllerComponents: ControllerComponents,
  val queryService: WrappedQueryService
)(
  implicit ec: ExecutionContext
)
extends BaseController
with RequestOps
{

  private val service = queryService.instance


  def getLocalQCReport: Action[AnyContent] = 
    Action.async {

      //TODO: get Querier and originating ZPM from request/session
      val querier = Querier("TODO")
      val origin  = ZPM("TODO")

      for {
        qc      <- service.getLocalQCReportFor(origin,querier)
        outcome = qc.leftMap(List(_))
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
 
    }
 
 
  def getGlobalQCReport: Action[AnyContent] = 
    Action.async {

      //TODO: get Querier from request/session
      val querier = Querier("TODO")

      for {
        qc     <- service compileGlobalQCReport querier
        outcome = qc.leftMap(_.toList)
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
    }


  //---------------------------------------------------------------------------
  // Query commands
  //---------------------------------------------------------------------------

  import QueryOps.Command._  


  def submit: Action[AnyContent] = 
    JsonAction[QueryForm]{ 
      case QueryForm(mode,params) => {

        //TODO: get Querier from request/session
        val querier = Querier("TODO")

        for {
          resp    <- service ! Submit(querier,mode,params)
          outcome =  resp.leftMap(errs => Outcome.fromErrors(errs.toList))
          result  =  outcome.toJsonResult
        } yield result
      }
    }
/*
    Action.async { implicit req =>
      
      //TODO: get Querier from request/session
      val querier = Querier("TODO")

      processJson[QueryForm]{ 
        case QueryForm(mode,params) => 
          for {
            resp    <- service ! Submit(querier,mode,params)
            outcome =  resp.leftMap(errs => Outcome.fromErrors(errs.toList))
            result  =  outcome.toJsonResult
          } yield result
      }
    }
*/ 
 
  def update(
    id: String
  ): Action[AnyContent] = 
    JsonAction[Update]{
      update => 
        for {
          updated <- service ! update
          outcome =  updated.leftMap(errs => Outcome.fromErrors(errs.toList))
          result  =  outcome.toJsonResult
        } yield result
    }

/*
    Action.async { implicit req =>
      
      processJson[Update]{
        update => 
          for {
            updated <- service ! update
            outcome =  updated.leftMap(errs => Outcome.fromErrors(errs.toList))
            result  =  outcome.toJsonResult
          } yield result
      }

    }
*/ 

  def applyFilter(
    id: String
  ): Action[AnyContent] = 
    JsonAction[ApplyFilter]{
      filter => 
        for {
          filtered <- service ! filter
          outcome  =  filtered.leftMap(errs => Outcome.fromErrors(errs.toList))
          result   =  outcome.toJsonResult
        } yield result
    }
/*
    Action.async { implicit req =>
      
      processJson[ApplyFilter]{
        filter => 
          for {
            filtered <- service ! filter
            outcome  =  filtered.leftMap(errs => Outcome.fromErrors(errs.toList))
            result   =  outcome.toJsonResult
          } yield result
      }
    }
*/
 
  //---------------------------------------------------------------------------
  // Query data access queries
  //---------------------------------------------------------------------------

  def query(
    id: String
  ): Action[AnyContent] = 
    Action.async {
      OptionT(service.get(Query.Id(id)))
        .map(Json.toJson(_))
        .fold(
          NotFound(s"Invalid Query ID $id")
        )(       
          Ok(_)
        )      
    }


  private def resultOf[T: Format](
    id: String
  )(
    rs: Future[Option[Iterable[T]]]
  ): Future[Result] = {
    OptionT(rs)
      .map(SearchSet(_))
      .map(Json.toJson(_))
      .fold(
        NotFound(s"Invalid Query ID $id")
      )(       
        Ok(_)
      )      
  }

  def patientsFrom(
    id: String
  ): Action[AnyContent] = 
    Action.async {
      resultOf(id)(service patientsFrom Query.Id(id))
    }


  def mtbfileFrom(
    id: String,
    patId: String
  ): Action[AnyContent] = 
    Action.async {
      OptionT(service mtbFileFrom (Query.Id(id),Patient.Id(patId)))
        .map(Json.toJson(_))
        .fold(
          NotFound(s"Invalid Query ID $id or Patient ID $patId")
        )(       
          Ok(_)
        )      
    }


/*
  def therapyRecommendationsFrom(
    id: String
  ): Action[AnyContent] = 
    Action.async {
      resultOf(id)(queryService.therapyRecommendationsFrom(Query.Id(id)))
    }

  def ngsSummariesFrom(
    id: String
  ): Action[AnyContent] = 
    Action.async {
      resultOf(id)(queryService.ngsSummariesFrom(Query.Id(id)))
    }
*/



  //---------------------------------------------------------------------------
  // Peer-to-peer operations
  //---------------------------------------------------------------------------
  def processPeerToPeerQuery: Action[AnyContent] = 
    JsonAction[PeerToPeerQuery]{
      query =>
        service.resultsOf(query)
          .map(SearchSet(_))
          .map(Json.toJson(_))
          .map(Ok(_))
    }
/*
    Action.async { implicit req =>

      processJson[PeerToPeerQuery]{
        query =>
          service.resultsOf(query)
            .map(SearchSet(_))
            .map(Json.toJson(_))
            .map(Ok(_))
      }
    }
*/

}
