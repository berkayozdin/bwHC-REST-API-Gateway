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




class APIBaseController @Inject()
(
  val controllerComponents: ControllerComponents,
  val sessionManager: WrappedSessionManager
)(
  implicit ec: ExecutionContext
)
extends BaseController
{
  import de.bwhc.rest.auth.UserHypermedia
  import de.bwhc.catalogs.api.CatalogHypermedia
  import de.bwhc.mtb.api.DataManagementPermissions.DataQualityAccessRights
  import de.bwhc.mtb.api.QueryPermissions.{
    QCAccessRight,
    EvidenceQueryRight
  }

  import Relations.SELF


  private val BASE_URI = "/bwhc"

  def ApiBase: Action[AnyContent] =
    Action.async {
      request =>

        val apiBase =
          Resource.empty
            .withLinks(
              SELF             -> Link(BASE_URI),
              "synthetic-data" -> Link("/bwhc/fake/data/api/MTBFile"),  //TODO: clean up
              "catalogs"       -> Link(CatalogHypermedia.BASE_URI)
            )

        for {
          optUser <-
            sessionManager.instance authenticate request

          api =
            optUser match {

              case None =>
                apiBase.withActions(UserHypermedia.LoginAction) 

              case Some(user) => {
                apiBase.withActions(
                  UserHypermedia.WhoAmIAction,
                  UserHypermedia.LogoutAction
                ) 
              }

            }

          result = Ok(toJson(api))

        } yield result

    }


}
