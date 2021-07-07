package de.bwhc.rest.auth



import javax.inject.Inject

import play.api.mvc.Results
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import play.api.libs.json.Json.toJson

import de.bwhc.user.api.{
  User, Role
}



class Router @Inject()(
  userController: UserController
)
extends SimpleRouter
{

  override def routes: Routes = {


    //-------------------------------------------------------------------------
    // Login / Logout
    //-------------------------------------------------------------------------
    case POST(p"/login")           => userController.login
    case POST(p"/logout")          => userController.logout
    case GET(p"/whoami")           => userController.whoAmI


    //-------------------------------------------------------------------------
    // User management 
    //-------------------------------------------------------------------------
    case GET(p"/")                 => userController.apiHypermedia

 
    case GET(p"/schema/$rel")      => userController.Action {
                                        Schemas.forRelation(rel)
                                          .map(Results.Ok(_))
                                          .getOrElse(Results.NotFound)
                                      }


    case GET(p"/user-roles")        => userController.Action {
                                      Results.Ok(toJson(Role.values))
                                    }

    case GET(p"/users")             => userController.getAll
    case GET(p"/users/$id")         => userController.get(User.Id(id))
                                   
    case POST(p"/users")            => userController.create
    case PUT(p"/users/$id")         => userController.update
    case PUT(p"/users/$id/roles")   => userController.updateRoles
    case DELETE(p"/users/$id")      => userController.delete(User.Id(id))

  }


}
