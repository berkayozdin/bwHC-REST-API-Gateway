package de.bwhc.mtb.api


import scala.util.{Either,Left,Right}
import scala.util.chaining._
import scala.concurrent.{
  Future,
  ExecutionContext
}
import javax.inject.Inject
import akka.util.ByteString
import play.api.http.Writeable
import play.api.mvc.{
  Action,
  Accepting,
  AnyContent,
  BaseController,
  ControllerComponents,
  Request,
  Result
}
import play.api.libs.json.{
  Json, Format, Writes
}
import Json.toJson
import de.bwhc.mtb.data.entry.dtos.{
  Coding,
  Medication,
  MTBFile,
  Patient,
  ZPM
}
import de.bwhc.mtb.data.entry.views.MTBFileView
import de.bwhc.mtb.query.api._
import de.bwhc.user.api.User
import cats.data.{
  EitherT,
  OptionT,
  Ior
}
import cats.instances.future._
import cats.syntax.either._
import cats.syntax.ior._
import de.bwhc.rest.util.{Outcome,RequestOps,SearchSet}
import de.bwhc.auth.api._
import de.bwhc.auth.core._
import de.bwhc.services.{WrappedQueryService,WrappedSessionManager}
import de.bwhc.rest.util.sapphyre.Hyper
import de.bwhc.rest.util.sapphyre.playjson._



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

  import QueryPermissions._


  implicit val authService = sessionManager.instance

  implicit val service = queryService.instance


  def ReportingApi: Action[AnyContent] =
    AuthenticatedAction.async {
      request =>
      
      implicit val user = request.user

      for {
        api    <- ReportingHypermedia.ApiResource
        result =  Ok(toJson(api))
      } yield result

    }

  def getLocalQCReport: Action[AnyContent] = 
    AuthenticatedAction( LocalQCAccessRight ).async {

      request =>

      val querier =
        Querier(request.user.userId.value)

      //TODO: get originating ZPM from request/session
      val origin  = ZPM("Local")

      for {
                   // re-use the local report generation function from the peer-to-peer usage
        qc      <- service.getLocalQCReport(PeerToPeerRequest(origin,querier))
        outcome = qc.leftMap(List(_))
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
 
    }
 
 
  def getGlobalQCReport: Action[AnyContent] = 
    AuthenticatedAction( GlobalQCAccessRight ).async {

      request =>

      implicit val querier =
        Querier(request.user.userId.value)

      for {
        qc     <- service.compileGlobalQCReport
        outcome = qc.leftMap(_.toList)
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
    }


  def getGlobalMedicationDistribution: Action[AnyContent] = 
    AuthenticatedAction( GlobalQCAccessRight ).async {
      request =>

      implicit val querier =
        Querier(request.user.userId.value)

      for {
        qc     <- service.compileGlobalMedicationDistribution
        outcome = qc.leftMap(_.toList)
                    .leftMap(Outcome.fromErrors)
        result  = outcome.toJsonResult
      } yield result
    }


  def getGlobalTumorEntityDistribution(
    optCode: Option[Medication.Code],
    optVersion: Option[String]
  ): Action[AnyContent] = 
    AuthenticatedAction( GlobalQCAccessRight ).async {
      request =>

      implicit val querier =
        Querier(request.user.userId.value)

      (optCode,optVersion) match {
        case (Some(code),None)    =>
          Future.successful(BadRequest(s"Only 'code' defined, missing query parameter 'version'"))

        case (None,Some(version)) =>
          Future.successful(BadRequest(s"Only 'version' defined, missing query parameter 'code'"))

        case _ => {  
          val coding = 
            for {
              code    <- optCode
              version <- optVersion       
            } yield
              Medication.Coding(
                code,
                Medication.System.ATC,
                None,
                Some(version)
              )
          
          for {
            qc     <- service.compileGlobalTumorEntityDistribution(coding)
            outcome = qc.leftMap(_.toList)
                        .leftMap(Outcome.fromErrors)
            result  = outcome.toJsonResult
          } yield result
        }
      }
    }


  import de.bwhc.util.csv._

  private val TEXT_CSV = "text/csv"

  implicit val csvDelimiter =
    Delimiter.Pipe

  implicit val csvWriteable =
    Writeable[Seq[CsvValue]](
      csvs =>
        csvs.foldLeft(
          ByteString.empty
        )(
          (acc,csv) =>
            acc ++ ByteString(s"${csv.toCsvString}\n","UTF-8")
        ),
      Some(TEXT_CSV)
    )

  private def toCsvWithHeader[T](
    ts: Seq[T]
  )(
    implicit csv: CsvWriter[T]
  ): Seq[CsvValue] = {
    import de.bwhc.util.csv.Csv.syntax._

    csv.headers +: ts.toCsv
  }


  def getPatientTherapies(
    optCode: Option[Medication.Code],
    optVersion: Option[String]
  ): Action[AnyContent] = 
    AuthenticatedAction( GlobalQCAccessRight ).async {
      request =>

      import java.io.StringWriter
      import PatientTherapies.csv._
      import scala.util.chaining._


      implicit val querier = Querier(request.user.userId.value)

      (optCode,optVersion) match {
        case (Some(code),None)    =>
          Future.successful(BadRequest(s"Only 'code' defined, missing query parameter 'version'"))

        case (None,Some(version)) =>
          Future.successful(BadRequest(s"Only 'version defined', missing query parameter 'code'"))

        case _ => {  
          val coding = 
            for {
              code    <- optCode
              version <- optVersion       
            } yield
              Medication.Coding(
                code,
                Medication.System.ATC,
                None,
                Some(version)
              )
          
          for {
            qc     <- service.compileGlobalPatientTherapies(coding)
            outcome = qc.leftMap(_.toList)
                        .leftMap(Outcome.fromErrors)
            
//            result  = outcome.toJsonResult
            
            result  = 
              if (request.acceptedTypes.find(_.mediaSubType.contains("csv")).isDefined){ 
                outcome.fold(
                  err =>
                    InternalServerError(Json.toJson(err)),
                  rep =>
                    Ok(
                      rep.data
                         .flatMap(denormalize)
                         .pipe(toCsvWithHeader(_))
                    ),
                  (_,rep) =>
                    Ok(
                      rep.data
                         .flatMap(denormalize)
                         .pipe(toCsvWithHeader(_))
                    ),
                )
              } else {
                outcome.toJsonResult
              }

          } yield result
        }
      }
    }


  def mtbfile(
    patId: String,
    site: Option[String],
    snapshot: Option[String]
  ): Action[AnyContent] = 
    AuthenticatedAction( MTBFileAccessRight ).async {
      
      request =>

      implicit val querier =
        Querier(request.user.userId.value)

      for {
        errOrSnp <-
          service.retrieveMTBFileSnapshot(
            Patient.Id(patId),
            snapshot.map(Snapshot.Id(_)),
            site.map(ZPM(_))
          )

        result = 
          errOrSnp match {
            case Left(err)         => InternalServerError(Json.toJson(Outcome.fromErrors(List(err))))

            case Right(Some(mtbf)) => Ok(Json.toJson(mtbf))

            case Right(None)       => NotFound("Invalid Patient ID or Snapshot ID")
          }
      } yield result

    }

  //---------------------------------------------------------------------------
  // Prepared Query Operations
  //---------------------------------------------------------------------------
  
  import QueryHypermedia._

  def savePreparedQuery =
    AuthenticatedAction( EvidenceQueryRight ).async {

      request => 

      implicit val querier =
        Querier(request.user.userId.value)

      errorsOrJson[PreparedQuery.Create]
        .apply(request)
        .fold(
          Future.successful,
          cmd =>
            (service ! cmd)
              .map(
                _.leftMap(_.toList)
                 .leftMap(Outcome.fromErrors)
                 .map(HyperPreparedQuery(_))
                 .toJsonResult
              )
        ) 
  }


  def getPreparedQueries =
    AuthenticatedAction( EvidenceQueryRight ).async {
      request => 

      import de.bwhc.rest.util.sapphyre.syntax._

      implicit val querier =
        Querier(request.user.userId.value)

      service.preparedQueries
        .map(
          _.bimap(
            List(_),
            _.map(HyperPreparedQuery(_))
          )
          .bimap(
            Outcome.fromErrors,
            SearchSet(_)
              .withActions(CreatePreparedQueryAction)
          )
          .toJsonResult
        )
    }


  def getPreparedQuery(id: PreparedQuery.Id) =
    AuthenticatedAction( EvidenceQueryRight ).async {
      request => 

      implicit val querier =
        Querier(request.user.userId.value)

      service.preparedQuery(id)
        .map(
          _.leftMap(List(_))
           .leftMap(Outcome.fromErrors)
           .leftMap(Json.toJson(_))
           .fold(
             InternalServerError(_),
             _.map(HyperPreparedQuery(_))
              .map(Json.toJson(_))
              .map(Ok(_))
              .getOrElse(NotFound(s"Invalid PreparedQuery ID ${id.value}"))
           )
        )
    }



  def updatePreparedQuery(id: PreparedQuery.Id) =
    AuthenticatedAction( EvidenceQueryRight ).async {

      request => 

      implicit val querier =
        Querier(request.user.userId.value)

      errorsOrJson[PreparedQuery.Update]
        .apply(request)
        .fold(
          Future.successful,
          cmd =>
            (service ! cmd)
              .map(
                _.leftMap(_.toList)
                 .bimap(
                   Outcome.fromErrors,
                   HyperPreparedQuery(_)
                 )
                 .toJsonResult
              )
        ) 
  }

  def deletePreparedQuery(id: PreparedQuery.Id) =
    AuthenticatedAction( EvidenceQueryRight ).async {

      request => 

      implicit val querier =
        Querier(request.user.userId.value)

      (service ! PreparedQuery.Delete(id))
        .map(
          _.leftMap(_.toList)
           .leftMap(Outcome.fromErrors)
           .toJsonResult
        )
      
  }


  //---------------------------------------------------------------------------
  // Query commands
  //---------------------------------------------------------------------------

  import QueryOps.Command


  def submit: Action[AnyContent] =
    AuthenticatedAction( EvidenceQueryRight ).async {

      request => 

      implicit val user = request.user
      implicit val querier = Querier(user.userId.value)

      errorsOrJson[Command.Submit]
        .apply(request)
        .fold(
          Future.successful,
          {
            case cmd @ Command.Submit(mode,params) =>
              for {         
                allowed <- user has QueryRightFor(mode.code)
            
                result <-
                  if (allowed)
                    for {
                      resp    <- service ! cmd
                      outcome <- resp.leftMap(
                                   errs => Outcome.fromErrors(errs.toList)
                                 )
                                 .fold(
                                   out     => Future.successful(out.leftIor),
                                   q       => HyperQuery(q).map(_.rightIor),
                                   (out,q) => HyperQuery(q).map(Ior.both(out,_))
                                 )
                      result  = outcome.toJsonResult
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
    AuthenticatedAction( AccessRightFor(id) ).async {

      request => 

      implicit val user = request.user
      implicit val querier = Querier(user.userId.value)

      errorsOrJson[Command.Update].apply(request)
        .fold(
          Future.successful,

          update => 
            for {         
              queryModeAllowed <- user has QueryRightFor(update.mode.code)

              result <-
                if (queryModeAllowed)
                  for {
                    resp    <- service ! update
                    outcome <- resp.leftMap(
                                 errs => Outcome.fromErrors(errs.toList)
                               )
                               .fold(
                                 out     => Future.successful(out.leftIor),
                                 q       => HyperQuery(q).map(_.rightIor),
                                 (out,q) => HyperQuery(q).map(Ior.both(out,_))
                               )
                    result  = outcome.toJsonResult
                  } yield result
                else 
                  Future.successful(Forbidden)
                
            } yield result
    
        )

    }


  def applyFilters(
    id: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(id) )
      .async {

        request => 
  
        implicit val user = request.user
        implicit val querier = Querier(user.userId.value)

        errorsOrJson[Command.ApplyFilters].apply(request)
          .fold(
            Future.successful,
  
            applyFilter => 
              for {
                resp    <- service ! applyFilter
                outcome <- resp.leftMap(
                             errs => Outcome.fromErrors(errs.toList)
                           )
                           .fold(
                             out     => Future.successful(out.leftIor),
                             q       => HyperQuery(q).map(_.rightIor),
                             (out,q) => HyperQuery(q).map(Ior.both(out,_))
                           )
                result  =  outcome.toJsonResult
              } yield result
      
          )
  
      }
 

  //---------------------------------------------------------------------------
  // Query data access queries
  //---------------------------------------------------------------------------

  def QueryApi: Action[AnyContent] =
    AuthenticatedAction.async {
      request =>
      
      implicit val user = request.user

      for {
        api    <- QueryHypermedia.Api
        result =  Ok(toJson(api))
      } yield result

    }


  def query(
    queryId: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) )
      .async { request =>

        implicit val user = request.user

        OptionT(service.get(queryId))
          .flatMapF(HyperQuery(_).map(Some(_)))
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value}")
          )(       
            Ok(_)
          )

      }


  private def resultOf[T: Writes](
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


  def resultSummaryFrom(
    queryId: Query.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) )
      .async {        
        OptionT(service.resultSummaryOf(queryId))
          .map(HyperResultSummary(_)(queryId))
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
    AuthenticatedAction( AccessRightFor(queryId) AND MTBFileAccessRight )
      .async {        
        OptionT(service patientsFrom queryId)
          .map(_.map(HyperPatient(_)(queryId)))
          .map(SearchSet(_))
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value}")
          )(       
            Ok(_)
          )      
      }


  def therapyRecommendationsFrom(
    queryId: Query.Id,
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) AND MTBFileAccessRight )
      .async {
        resultOf(service therapyRecommendationsFrom queryId)(queryId)
      }


  def molecularTherapiesFrom(
    queryId: Query.Id,
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) AND MTBFileAccessRight )
      .async {
        resultOf(service molecularTherapiesFrom queryId)(queryId)
      }


  def ngsSummariesFrom(
    queryId: Query.Id,
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) AND MTBFileAccessRight )
      .async {
        resultOf(service.ngsSummariesFrom(queryId))(queryId)
      }


  def variantsOfInterestOf(
    queryId: Query.Id,
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) AND MTBFileAccessRight )
      .async {
        OptionT(service variantsOfInterestOf queryId)
          .map(HyperVariantsOfInterest(_)(queryId))
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value}")
          )(       
            Ok(_)
          )      
      }


  def mtbfileFrom(
    queryId: Query.Id,
    patId: Patient.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) AND MTBFileAccessRight )
      .async {
        OptionT(service.mtbFileFrom(queryId,patId))
          .map(HyperMTBFile(_)(queryId))
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value} or Patient ID $patId")
          )(       
            Ok(_)
          )      
      }


  def mtbfileViewFrom(
    queryId: Query.Id,
    patId: Patient.Id
  ): Action[AnyContent] = 
    AuthenticatedAction( AccessRightFor(queryId) AND MTBFileAccessRight )
      .async {
        OptionT(service.mtbFileViewFrom(queryId,patId))
          .map(HyperMTBFileView(_)(queryId))
          .map(Json.toJson(_))
          .fold(
            NotFound(s"Invalid Query ID ${queryId.value} or Patient ID $patId")
          )(       
            Ok(_)
          )      
      }



}
