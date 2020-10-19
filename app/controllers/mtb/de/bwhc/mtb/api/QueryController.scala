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
  Json, Format
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

import de.bwhc.auth.api._
import de.bwhc.auth.core._
import de.bwhc.auth.core.Authorization._

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
{

  import Authorizations._


  implicit val authService = sessionManager.instance

  private val service = queryService.instance


  //TODO: Check how to distinguish locally issued LocalQCReport query from externally issued for GlobalQCReport compilation
  def getLocalQCReport: Action[AnyContent] = 
    AuthenticatedAction( LocalQCAccessRights ).async {

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
    AuthenticatedAction( GlobalQCAccessRights ).async {

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
    AuthenticatedAction( EvidenceQueryRights ).async {

      request => 

      val user = request.user

      errorsOrJson[QueryForm]
        .apply(request)
        .fold(
          Future.successful,
          {
            case QueryForm(mode,params) =>
              for {         
                allowed <- user has QueryRightsFor(mode)
            
                result <-
                  if (allowed)
                    for {
                      resp    <- service ! Command.Submit(Querier(user.userId.value),mode,params)
                      outcome =  resp.leftMap(errs => Outcome.fromErrors(errs.toList))
                      result  =  outcome.toJsonResult
                    } yield result
                  else 
                    Future.successful(Forbidden)
                  
              } yield result
          }
        )

   }


  private def QueryRightsFor(
    mode: Query.Mode.Value
  ): Authorization[UserWithRoles] =
    if (mode == Query.Mode.Federated) FederatedEvidenceQueryRights
    else LocalEvidenceQueryRights
 


  implicit val userIsQuerySubmitter: (User.Id,Query.Id) => Future[Boolean] = {
    (userId,queryId) =>
       for {
         query <- service get queryId
         ok    =  query.exists(_.querier.value == userId.value)
       } yield ok

  }


  def update(
    id: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction(EvidenceQueryRights).async {

      request => 

      val user = request.user

      errorsOrJson[Command.Update].apply(request)
        .fold(
          Future.successful,

          update => 
            for {         
              queryModeAllowed <- user has QueryRightsFor(update.mode)
           
              isQuerySubmitter <- user has ResourceOwnership(update.id) 

              allowed = (queryModeAllowed && isQuerySubmitter)

              result <-
                if (allowed)
                  for {
                    resp    <- service ! update
                    outcome =  resp.leftMap(errs => Outcome.fromErrors(errs.toList))
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
    AuthenticatedAction( EvidenceQueryRights and ResourceOwnership(id) )
      .async {
  
        request => 
  
        val user = request.user
  
        errorsOrJson[Command.ApplyFilter]
          .apply(request)
          .fold(
            Future.successful,
  
            applyFilter => 
              for {
                resp    <- service ! applyFilter
                outcome =  resp.leftMap(errs => Outcome.fromErrors(errs.toList))
                result  =  outcome.toJsonResult
              } yield result
      
          )
  
      }
/*
    AuthenticatedAction( EvidenceQueryRights ).async {

      request => 

      val user = request.user

      errorsOrJson[Command.ApplyFilter].apply(request)
        .fold(
          Future.successful,

          update => 
            for {         
           
              isQuerySubmitter <- user has ResourceOwnership(update.id) 

              result <-
                if (!isQuerySubmitter) Future.successful(Forbidden)
                else {
                  for {
                    resp    <- service ! update
                    outcome =  resp.leftMap(errs => Outcome.fromErrors(errs.toList))
                    result  =  outcome.toJsonResult
                  } yield result
                }
            } yield result
    
        )

    }
*/
 
  //---------------------------------------------------------------------------
  // Query data access queries
  //---------------------------------------------------------------------------



  def query(
    queryId: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRights and ResourceOwnership(queryId) )
      .async {
        OptionT(service get queryId)
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value}")
          )(       
            Ok(_)
          )      
      }


  private def resultOf[T: Format](
    queryId: Query.Id
  )(
    rs: Future[Option[Iterable[T]]]
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
    AuthenticatedAction( EvidenceQueryRights and ResourceOwnership(queryId) )
      .async {
        resultOf(queryId)(service patientsFrom queryId)
      }


  def mtbfileFrom(
    queryId: Query.Id,
    patId: String
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQueryRights and ResourceOwnership(queryId) )
      .async {
        OptionT(service mtbFileFrom (queryId,Patient.Id(patId)))
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value} or Patient ID $patId")
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

}
