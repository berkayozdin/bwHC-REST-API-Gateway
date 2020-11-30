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

import de.bwhc.services.WrappedSessionManager

import de.bwhc.rest.util.sapphyre._
import de.bwhc.rest.util.sapphyre.playjson._

import de.bwhc.util.syntax.piping._


class APIBaseController @Inject()
(
  val controllerComponents: ControllerComponents,
  val sessionManager: WrappedSessionManager
)(
  implicit ec: ExecutionContext
)
extends BaseController
{

  import de.bwhc.systems.api.SystemHypermedia
  import de.bwhc.rest.auth.UserHypermedia
  import de.bwhc.catalogs.api.CatalogHypermedia
  import de.bwhc.mtb.api.DataManagementPermissions.DataQualityAccessRights
  import de.bwhc.mtb.api.QueryPermissions.{
    QCAccessRight,
    EvidenceQueryRight
  }
  import de.bwhc.mtb.api.{
    ReportingHypermedia,
    DataQualityHypermedia,
    QueryHyperResources  //TODO: clean up
  }

  import Relations.SELF


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
        "systems-api"    -> SystemHypermedia.ApiBaseLink
      )


  def ApiBase: Action[AnyContent] =
    Action.async {
      request =>

        for {
          optUser <-
            sessionManager.instance authenticate request

          apiDef <-
            optUser match {

              case None =>
                Future.successful(
                  api.withActions(UserHypermedia.LoginAction)
                ) 

              case Some(user) => {

                for {

                  dataQCAccess    <- user has DataQualityAccessRights
                  reportingAccess <- user has QCAccessRight
                  queryAccess     <- user has EvidenceQueryRight

                  result =
                    api.withActions(
                      UserHypermedia.WhoAmIAction,
                      UserHypermedia.LogoutAction
                    )
                    .withLinks("user-api" -> UserHypermedia.ApiBaseLink) |
                    (r => if (dataQCAccess)
                             r.withLinks(DATA_QC_API   -> DataQualityHypermedia.ApiBaseLink)
                          else r) |
                    (r => if (reportingAccess)
                             r.withLinks(REPORTING_API -> ReportingHypermedia.ApiBaseLink)
                          else r) |  
                    (r => if (queryAccess)  
                             r.withLinks(QUERY_API     -> QueryHyperResources.ApiBaseLink)
                          else r)   
                   
                } yield result

              }
            }

          result = Ok(toJson(apiDef))

        } yield result

    }



}
