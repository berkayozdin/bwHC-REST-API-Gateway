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



//object QueryModePermissions
trait QueryModePermissions
{

  import de.bwhc.user.api.Role._


  val LocalQCAccess =
    Authorization[UserWithRoles](user =>
      (user hasRole LocalZPMCoordinator) ||
      (user hasRole GlobalZPMCoordinator) ||
      (user hasRole MTBCoordinator)
    )


  val GlobalQCAccess =
    Authorization[UserWithRoles](_ hasRole GlobalZPMCoordinator)


  val FederatedEvidenceQuery =
    Authorization[UserWithRoles](
      _ hasAnyOf Set(GlobalZPMCoordinator, Researcher)
    )


  val LocalEvidenceQuery =
    Authorization[UserWithRoles](
      _ hasAnyOf Set(GlobalZPMCoordinator, Researcher, LocalZPMCoordinator, MTBCoordinator)
    )

  val EvidenceQuery = LocalEvidenceQuery


  def QueryRightsFor(
    mode: Query.Mode.Value
  ): Authorization[UserWithRoles] =
    if (mode == Query.Mode.Federated) FederatedEvidenceQuery
    else LocalEvidenceQuery
 

  protected val service: QueryService


  def AccessRightsFor(
    queryId: Query.Id
  )(
    implicit ec: ExecutionContext
  ): Authorization[UserWithRoles] = Authorization.async{

    case UserWithRoles(userId,_) =>

      for {
        query <- service get queryId
        ok    =  query.exists(_.querier.value == userId.value)
      } yield ok
  }


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


  implicit val authService = sessionManager.instance

  protected val service = queryService.instance


  //TODO: Check how to distinguish locally issued LocalQCReport query from externally issued for GlobalQCReport compilation
  def getLocalQCReport: Action[AnyContent] = 
    AuthenticatedAction( LocalQCAccess ).async {

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
    AuthenticatedAction( GlobalQCAccess ).async {

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
    AuthenticatedAction( EvidenceQuery ).async {

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


  def update(
    id: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQuery AND AccessRightsFor(id) ).async {

      request => 

      val user = request.user

      errorsOrJson[Command.Update].apply(request)
        .fold(
          Future.successful,

          update => 
            for {         
              queryModeAllowed <- user has QueryRightsFor(update.mode)

              result <-
                if (queryModeAllowed)
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
    AuthenticatedAction( EvidenceQuery AND AccessRightsFor(id) )
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
 
  //---------------------------------------------------------------------------
  // Query data access queries
  //---------------------------------------------------------------------------


  def query(
    queryId: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQuery and AccessRightsFor(queryId) )
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
    AuthenticatedAction( EvidenceQuery and AccessRightsFor(queryId) )
      .async {
        resultOf(queryId)(service patientsFrom queryId)
      }


  def mtbfileFrom(
    queryId: Query.Id,
    patId: String
  ): Action[AnyContent] = 
    AuthenticatedAction( EvidenceQuery and AccessRightsFor(queryId) )
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


}
