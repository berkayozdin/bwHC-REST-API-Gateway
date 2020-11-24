package de.bwhc.rest.auth



import scala.util.{
  Either,
  Left,
  Right
}
import scala.concurrent.{
  Future,
  ExecutionContext
}

import javax.inject.Inject

import play.api.mvc.{
  Action,
  AnyContent,
  BodyParsers,
  BaseController,
  ControllerComponents,
  Request,
  Result
}
import play.api.libs.json.Json
import Json.toJson

import cats.data.NonEmptyList
import cats.syntax.either._



import de.bwhc.rest.util.{
  Outcome,
  RequestOps,
  SearchSet
}
import de.bwhc.rest.util.cphl.syntax._

import de.bwhc.user.api.{
  User,
  UserCommand,
  UserEvent,
  UserService,
  Role
}

import de.bwhc.auth.core._
import de.bwhc.auth.api._
import de.bwhc.services.{WrappedSessionManager,WrappedUserService}

import de.bwhc.rest.util.sapphyre.playjson._


case class Credentials
(
  username: User.Name,
  password: User.Password
)

object Credentials
{
  implicit val reads = Json.reads[Credentials]
}



class UserController @Inject()(
  val controllerComponents: ControllerComponents,
  val sessionManager: WrappedSessionManager,
  val userService: WrappedUserService
)(
  implicit ec: ExecutionContext
)
extends BaseController
with RequestOps
with AuthenticationOps[UserWithRoles]
{

  import UserManagementPermissions._
  import UserHypermedia._


  implicit val authService = sessionManager.instance


  def apiHypermedia: Action[AnyContent] = 
    AuthenticatedAction.async {
      req =>

      ApiResource(req.user)
        .map(toJson(_))
        .map(Ok(_))
    }


  def login: Action[AnyContent] =
    Action.async { implicit request =>

      val body = request.body
     
      val credentials: Either[Result,Credentials] =
        body
          .asJson
          .map(
            _.validate[Credentials]
             .asEither
             .leftMap(Outcome.fromJsErrors(_))
             .leftMap(toJson(_))
             .leftMap(BadRequest(_))
          )
          .orElse(
            body
              .asFormUrlEncoded
              .flatMap( form =>
                for {
                  username <- form.get("username").flatMap(_.headOption).map(User.Name(_))
                  password <- form.get("password").flatMap(_.headOption).map(User.Password(_))
                } yield Credentials(username,password).asRight[Result]
              )
          )
          .getOrElse(BadRequest("Invalid or missing Form body").asLeft[Credentials])

      credentials.fold(
        Future.successful(_),
        {
          case Credentials(username,password) =>
            for {
              optUser <- userService.instance.identify(username,password)
          
              result  <- optUser
                           .map( user =>
                              authService.login(
                                UserWithRoles(user.id,user.roles)
                              )
                            )
                         .getOrElse(Future.successful(Forbidden))
            } yield result
        }
      )

    }


  def logout: Action[AnyContent] =
    AuthenticatedAction.async { authService.logout(_) }


  def create: Action[AnyContent] = 
    AuthenticatedAction( CreateUserRights )
      .async {
        req =>

        (errorsOrJson[UserCommand.Create] thenApply (process(_)(req.user) )).apply(req)
//        errorsOrJson[UserCommand.Create] thenApply process
      }

  import de.bwhc.rest.util.sapphyre.playjson._

  def getAll: Action[AnyContent] =
    AuthenticatedAction( GetAllUserRights ).async {
      req =>

      for {
        users   <- userService.instance.getAll      
        result  <- UsersResource(users)(req.user)
                     .map(toJson(_))  
      } yield Ok(result)

    }


  def get(id: User.Id): Action[AnyContent] =
    AuthenticatedAction( ReadUserRights(id) )
      .async {

        req =>

        for {
          user   <- userService.instance.get(id)
          result <- user.map(
                      UserResource(_)(req.user)
                        .map(toJson(_))
                        .map(Ok(_))
                     )
                     .getOrElse(Future.successful(NotFound(s"Invalid UserId $id")))
        } yield result
      }


  def update: Action[AnyContent] =
    AuthenticatedAction.async {

      request => 

      val agent = request.user

      errorsOrJson[UserCommand.Update].apply(request)
        .fold(
          Future.successful,
          update => 
            for {
              allowed <- agent has UpdateUserRights(update.id)
              result <- if (allowed) process(update)(agent)
                        else Future.successful(Forbidden)
            } yield result
        )
    }


  def updateRoles: Action[AnyContent] =
    AuthenticatedAction( UpdateUserRolesRights )
      .async {
        req =>
        (errorsOrJson[UserCommand.UpdateRoles] thenApply (process(_)(req.user))).apply(req)
      }


  def delete(id: User.Id): Action[AnyContent] =
    AuthenticatedAction( DeleteUserRights )
      .async { req => 
        process(UserCommand.Delete(id))(req.user)
      }


  private def process(
    cmd: UserCommand
  )(
    agent: UserWithRoles
  ): Future[Result] =
    for {
      response <- userService.instance ! cmd
      result <-
        response.fold(
          errs => Future.successful(UnprocessableEntity(toJson(Outcome.fromErrors(errs.toList)))),
          {
            case UserEvent.Created(user,_)   => UserResource(user)(agent)
                                                  .map(toJson(_))
                                                  .map(Created(_))
                                             
            case UserEvent.Updated(user,_)   => UserResource(user)(agent)
                                                  .map(toJson(_))
                                                  .map(Ok(_))

            case UserEvent.Deleted(userId,_) => Future.successful(Ok)

          }
        )
    } yield result


}
