package de.bwhc.rest.auth


import javax.inject.Inject

import scala.concurrent.{ExecutionContext,Future}

import play.api.mvc.{
  Action,
  ActionBuilder,
  ActionTransformer,
  AnyContent,
  BodyParsers,
  WrappedRequest,
  Request
}

import de.bwhc.util.oauth.AccessToken
import de.bwhc.user.auth.api.UserWithRoles



// See: https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Action-composition

final class UserRequest[+T](
  val user: Option[UserWithRoles],
  val request: Request[T]
)
extends WrappedRequest(request)



final class UserAction @Inject()(
  val parser: BodyParsers.Default
)(
  implicit
  val executionContext: ExecutionContext,
  val services: Services
)
extends ActionBuilder[UserRequest, AnyContent]
with ActionTransformer[Request, UserRequest]
{

  val userService = services.userService

  override def transform[A](req: Request[A]) =
    
    req.session.get("sessionToken") match {

      case None =>
        Future.successful(new UserRequest(None,req))

      case Some(tkn) =>
        userService.sessionFor(AccessToken(tkn))
          .map(_.map(_.user))
          .map(new UserRequest(_,req))
    }

}

