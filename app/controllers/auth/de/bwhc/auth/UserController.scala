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


case class Credentials
(
  username: User.Name,
  password: User.Password
)

object Credentials
{
  implicit val reads = Json.reads[Credentials]
}


trait UserManagementPermissions
{

  import Role._

  private val AdminRights = Authorization[UserWithRoles](_ hasRole Admin)


  val CreateRights = AdminRights

  def ReadRights(id: User.Id) =
    Authorization[UserWithRoles](user =>
      (user hasRole Admin) || (user.userId == id)
    )

  def UpdateRights(id: User.Id) = ReadRights(id)

  val UpdateRolesRights = AdminRights

  val DeleteRights = AdminRights

  val GetAllRights = AdminRights

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
with UserManagementPermissions
{

  implicit val authService = sessionManager.instance


  def login: Action[AnyContent] =
    Action.async { implicit request =>

      val body = request.body
     
      val credentials: Either[Result,Credentials] =
        body
         .asFormUrlEncoded
         .flatMap( form =>
           for {
             username <- form.get("username").flatMap(_.headOption).map(User.Name(_))
             password <- form.get("password").flatMap(_.headOption).map(User.Password(_))
           } yield Credentials(username,password).asRight[Result]
         )
         .getOrElse(BadRequest("Invalid or missing Form body").asLeft[Credentials])
 
      credentials.fold(
        Future.successful(_),
        {
          case Credentials(username,password) =>
            for {
              optUser <- userService.instance.identify(username,password)
          
              result  <- optUser.map( 
                           user => authService.login(UserWithRoles(user.id,user.roles))
                         )
                         .getOrElse(Future.successful(Forbidden))
            } yield result
        }
      )

    }


  def logout: Action[AnyContent] =
    AuthenticatedAction.async { authService.logout(_) }


  def create = 
    AuthenticatedAction( CreateRights )
      .async {
        errorsOrJson[UserCommand.Create] thenApply processUserCommand
      }


  def getAll =
    AuthenticatedAction( GetAllRights ).async {
      for {
        users   <- userService.instance.getAll
        userSet =  SearchSet(users)
        result  =  toJson(userSet)
      } yield Ok(result)
    }


  def get(id: User.Id) =
    AuthenticatedAction( ReadRights(id) )
      .async {
        for {
          user   <- userService.instance.get(id)
          result =  user.map(toJson(_))
                      .map(Ok(_))
                      .getOrElse(NotFound(s"Invalid UserId $id"))
        } yield result
      }


  def update =
    AuthenticatedAction.async {

      request => 

      val user = request.user

      errorsOrJson[UserCommand.Update].apply(request)
        .fold(
          Future.successful,

          update => 
            for {

              allowed <- user has UpdateRights(update.id)

              result <- if (allowed) processUserCommand(update)
                        else Future.successful(Forbidden)

            } yield result
        )
    }


  def updateRoles =
    AuthenticatedAction( UpdateRolesRights )
      .async {
        errorsOrJson[UserCommand.UpdateRoles] thenApply processUserCommand
      }


  def delete(id: User.Id) =
    AuthenticatedAction( DeleteRights )
      .async {
        processUserCommand(UserCommand.Delete(id))
      }



  private def processUserCommand(
    cmd: UserCommand
  ): Future[Result] =
    for {
      response <- userService.instance ! cmd
      result =
        response.fold(
          errs => UnprocessableEntity(toJson(Outcome.fromErrors(errs.toList))),
          {
            case UserEvent.Created(user,_) => Created(toJson(user))

            case UserEvent.Updated(user,_) => Ok(toJson(user))

            case UserEvent.Deleted(userId,_) => Ok

          }
        )
    } yield result



}
