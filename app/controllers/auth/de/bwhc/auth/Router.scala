package de.bwhc.rest.auth



import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._



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
    case GET(p"/user")             => userController.getAll
    case GET(p"/user/$id")         => userController.get(User.Id(id))
                                   
    case POST(p"/user")            => userController.create
    case PUT(p"/user/$id")         => userController.update
    case PUT(p"/user/$id/roles")   => userController.updateRoles
    case DELETE(p"/user/$id")      => userController.delete(User.Id(id))


  }


}
