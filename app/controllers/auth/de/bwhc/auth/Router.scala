package de.bwhc.rest.auth



import javax.inject.Inject

import play.api.mvc.Results
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

//import play.api.libs.json.Json.toJson

import de.bwhc.user.api.User



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


    //-------------------------------------------------------------------------
    // User management 
    //-------------------------------------------------------------------------
    case GET(p"/")                 => userController.apiHypermedia


    case GET(p"/siren")            => userController.Action {
                                        Results.Ok(UserSIREN.apiBase)
                                      }

//    case GET(p"/users")            => userController.getAll


 
    case GET(p"/schema/$rel")      => userController.Action {
                                        UserCPHL.schemaFor(rel)
                                          .map(Results.Ok(_))
                                          .getOrElse(Results.NotFound)
                                      }


    case GET(p"/user")             => userController.getAll
    case GET(p"/user/$id")         => userController.get(User.Id(id))
                                   
    case POST(p"/user")            => userController.create
    case PUT(p"/user/$id")         => userController.update
    case PUT(p"/user/$id/roles")   => userController.updateRoles
    case DELETE(p"/user/$id")      => userController.delete(User.Id(id))

  }


}
