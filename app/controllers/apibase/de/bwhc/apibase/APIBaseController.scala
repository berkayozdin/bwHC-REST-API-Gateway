package de.bwhc.apibase



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
}
import play.api.libs.json.Json.toJson


import de.bwhc.auth.api._
import de.bwhc.auth.core._

import de.bwhc.services.{WrappedSessionManager,WrappedQueryService}

import de.bwhc.rest.util.sapphyre._
import de.bwhc.rest.util.sapphyre.playjson._

import de.bwhc.util.syntax.piping._


class APIBaseController @Inject()
(
  val controllerComponents: ControllerComponents,
  val sessionManager: WrappedSessionManager,
  val queryService: WrappedQueryService
)(
  implicit ec: ExecutionContext
)
extends BaseController
with AuthenticationOps[UserWithRoles]
{

  import de.bwhc.user.api.Role._
  import de.bwhc.etl.api.ETLHypermedia
  import de.bwhc.rest.auth.UserHypermedia
  import de.bwhc.rest.auth.UserManagementPermissions._
  import de.bwhc.catalogs.api.CatalogHypermedia
  import de.bwhc.mtb.api.DataManagementPermissions.DataQualityAccessRights
  import de.bwhc.mtb.api.QueryPermissions.{
    QCAccessRight,
    EvidenceQueryRight,
  }
  import de.bwhc.mtb.api.{
    ReportingHypermedia,
    DataQualityHypermedia,
    QueryHypermedia
  }

  import Relations.SELF


  implicit val authService = sessionManager.instance

  

  private val AdminRights =
    Authorization[UserWithRoles](_ hasRole Admin)



  private val BASE_URI = "/bwhc"

  private val DATA_QC_API   = "data-quality-api"
  private val REPORTING_API = "reporting-api"   
  private val QUERY_API     = "query-api"       

  private val api =
    Resource.empty
      .withLinks(
        SELF             -> Link(BASE_URI),
        "synthetic-data" -> Link("/bwhc/fake/data/api/MTBFile"),  //TODO: clean up
        "catalogs-api"   -> CatalogHypermedia.ApiBaseLink,
        "etl-api"        -> ETLHypermedia.ApiBaseLink
      )


  def ApiBase: Action[AnyContent] =
    Action.async {
      request =>

        for {
          optUser <-
            sessionManager.instance.authenticate(request)

          apiDef <-
            optUser match {

              case None =>
                Future.successful(
                  api.withActions(UserHypermedia.LoginAction)
                ) 

              case Some(user) => {

                for {

                  admin           <- user has AdminRights
                  dataQCAccess    <- user has DataQualityAccessRights
                  reportingAccess <- user has QCAccessRight
                  queryAccess     <- user has EvidenceQueryRight
                  allUsersAccess  <- user has GetAllUserRights

                  result =
                    api.withActions(
                      UserHypermedia.WhoAmIAction,
                      UserHypermedia.LogoutAction
                    )
                    .withLinks(
                      "userApi" -> UserHypermedia.ApiBaseLink,
                      "whoami"   -> UserHypermedia.UserLink(user.userId)
                    ) |
                    (r => if (admin)           r.withLinks("peerStatusReport" -> Link(s"$BASE_URI/peerStatusReport")) else r) |
                    (r => if (allUsersAccess)  r.withLinks(UserHypermedia.USERS -> UserHypermedia.UsersLink)          else r) |
                    (r => if (dataQCAccess)    r.withLinks(DATA_QC_API          -> DataQualityHypermedia.ApiBaseLink) else r) |
                    (r => if (reportingAccess) r.withLinks(REPORTING_API        -> ReportingHypermedia.ApiBaseLink)   else r) |  
                    (r => if (queryAccess)     r.withLinks(QUERY_API            -> QueryHypermedia.ApiBaseLink)       else r)   
                   
                } yield result

              }
            }

          result = Ok(toJson(apiDef))

        } yield result

    }



  def peerStatusReport: Action[AnyContent] =
    AuthenticatedAction(AdminRights).async {
      queryService.instance
        .peerStatusReport
        .map(toJson(_))
        .map(Ok(_))

    }


}
