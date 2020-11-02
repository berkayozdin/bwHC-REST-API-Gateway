package de.bwhc.mtb.api



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
  Json, Format, Writes
}

import de.bwhc.mtb.data.entry.dtos.{
  MTBFile,
  Patient,
  ZPM
}
import de.bwhc.mtb.query.api._

import de.bwhc.user.api.User

import cats.data.{
  EitherT,
  OptionT
}
import cats.instances.future._
import cats.syntax.either._

import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}
import de.bwhc.rest.util.cphl.syntax._

import de.bwhc.auth.api._
import de.bwhc.auth.core._

import de.bwhc.services.{WrappedQueryService,WrappedSessionManager}



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
  val queryService: WrappedQueryService,
  val sessionManager: WrappedSessionManager
)(
  implicit ec: ExecutionContext
)
extends BaseController
with RequestOps
with AuthenticationOps[UserWithRoles]
with QueryModePermissions
{

  import QueryHypermedia._


  implicit val authService = sessionManager.instance

  protected val service = queryService.instance


  def getLocalQCReport: Action[AnyContent] = 
    AuthenticatedAction( LocalQCAccessRight ).async {

      request =>

      val querier = Querier(request.user.userId.value)

      //TODO: get originating ZPM from request/session
      val origin  = ZPM("TODO")

      for {
        qc      <- service.getLocalQCReportFor(origin,querier)
        outcome = qc.leftMap(List(_))
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
 
    }
 
 
  def getGlobalQCReport: Action[AnyContent] = 
    AuthenticatedAction( GlobalQCAccessRight ).async {

      request =>

      val querier = Querier(request.user.userId.value)

      for {
        qc     <- service.compileGlobalQCReport(querier)
        outcome = qc.leftMap(_.toList)
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
    }


  //---------------------------------------------------------------------------
  // Query commands
  //---------------------------------------------------------------------------

  import QueryOps.Command


  def submit: Action[AnyContent] =
    AuthenticatedAction( EvidenceQueryRight ).async {

      request => 

      val user = request.user

      errorsOrJson[QueryForm]
        .apply(request)
        .fold(
          Future.successful,
          {
            case QueryForm(mode,params) =>
              for {         
                allowed <- user has QueryRightFor(mode)
            
                result <-
                  if (allowed)
                    for {
                      resp    <- service ! Command.Submit(Querier(user.userId.value),mode,params)
//                      outcome =  resp.leftMap(errs => Outcome.fromErrors(errs.toList))
                      outcome =  resp.bimap(
                                   errs => Outcome.fromErrors(errs.toList),
                                   _.withHypermedia
                                 )
                      result  =  outcome.toJsonResult
                    } yield result
                  else 
                    Future.successful(Forbidden)
                  
              } yield result
          }
        )

   }


  def update(
    id: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight AND AccessRightFor(id) ).async {

      request => 

      val user = request.user

      errorsOrJson[Command.Update].apply(request)
        .fold(
          Future.successful,

          update => 
            for {         
              queryModeAllowed <- user has QueryRightFor(update.mode)

              result <-
                if (queryModeAllowed)
                  for {
                    resp    <- service ! update
                    outcome =  resp.bimap(
                                 errs => Outcome.fromErrors(errs.toList),
                                 _.withHypermedia
                               )
                    result  =  outcome.toJsonResult
                  } yield result
                else 
                  Future.successful(Forbidden)
                
            } yield result
    
        )

    }


  def applyFilter(
    id: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight AND AccessRightFor(id) )
      .async {
  
        request => 
  
        errorsOrJson[Command.ApplyFilter].apply(request)
          .fold(
            Future.successful,
  
            applyFilter => 
              for {
                resp    <- service ! applyFilter
                outcome =  resp.bimap(
                             errs => Outcome.fromErrors(errs.toList),
                             _.withHypermedia
                           )
                result  =  outcome.toJsonResult
              } yield result
      
          )
  
      }
 
  //---------------------------------------------------------------------------
  // Query data access queries
  //---------------------------------------------------------------------------


  def query(
    queryId: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight and AccessRightFor(queryId) )
      .async {
        OptionT(service get queryId)
          .map(_.withHypermedia)
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value}")
          )(       
            Ok(_)
          )      
      }


  private def resultOf[T: Format](
    rs: Future[Option[Iterable[T]]]
  )(
    queryId: Query.Id
  ): Future[Result] = {
    OptionT(rs)
      .map(SearchSet(_))
      .map(Json.toJson(_))
      .fold(
        NotFound(s"Invalid Query ID ${queryId.value}")
      )(       
        Ok(_)
      )      
  }

  def patientsFrom(
    queryId: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight and AccessRightFor(queryId) )
      .async {
        resultOf(service patientsFrom queryId)(queryId)
      }


  def mtbfileFrom(
    queryId: Query.Id,
    patId: String
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight and AccessRightFor(queryId) )
      .async {
        OptionT(service mtbFileFrom (queryId,Patient.Id(patId)))
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value} or Patient ID $patId")
          )(       
            Ok(_)
          )      
      }



  def therapyRecommendationsFrom(
    queryId: Query.Id,
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight and AccessRightFor(queryId) )
      .async {
        resultOf(service therapyRecommendationsFrom queryId)(queryId)
      }


  def molecularTherapiesFrom(
    queryId: Query.Id,
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight and AccessRightFor(queryId) )
      .async {
        resultOf(service molecularTherapiesFrom queryId)(queryId)
      }


  def ngsSummariesFrom(
    queryId: Query.Id,
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRight and AccessRightFor(queryId) )
      .async {
        resultOf(service.ngsSummariesFrom(queryId))(queryId)
      }



}
