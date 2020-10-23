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

  private val AdminRights =
    Authorization[UserWithRoles](_ hasRole Admin)


  val CreateUserRights = AdminRights

  def ReadUserRights(id: User.Id) =
    Authorization[UserWithRoles](user =>
      (user.userId == id) || (user hasRole Admin) 
    )

  def UpdateUserRights(id: User.Id) = ReadUserRights(id)

  val UpdateUserRolesRights = AdminRights

  val DeleteUserRights = AdminRights

  val GetAllUserRights = AdminRights

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
    AuthenticatedAction( CreateUserRights )
      .async {
        errorsOrJson[UserCommand.Create] thenApply process
      }


  def getAll =
    AuthenticatedAction( GetAllUserRights ).async {
      for {
        users   <- userService.instance.getAll
        userSet =  SearchSet(users)
        result  =  toJson(userSet)
      } yield Ok(result)
    }


  import de.bwhc.rest.util.hal._
  import de.bwhc.rest.util.hal.Relations._
  import de.bwhc.rest.util.hal.syntax._


  implicit val addHypermedia: User => Hyper[User] = {
    user =>
      user.withLinks(
        Self -> s"/bwhc/user/api/user/${user.id.value}"
      )
  }


  def get(id: User.Id) =
    AuthenticatedAction( ReadUserRights(id) )
      .async {
        for {
          user   <- userService.instance.get(id)
          result =  user.map(_.withHypermedia)
                      .map(toJson(_))
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

              allowed <- user has UpdateUserRights(update.id)

              result <- if (allowed) process(update)
                        else Future.successful(Forbidden)

            } yield result
        )
    }


  def updateRoles =
    AuthenticatedAction( UpdateUserRolesRights )
      .async {
        errorsOrJson[UserCommand.UpdateRoles] thenApply process
      }


  def delete(id: User.Id) =
    AuthenticatedAction( DeleteUserRights )
      .async {
        process(UserCommand.Delete(id))
      }


  private def process(
    cmd: UserCommand
  ): Future[Result] =
    for {
      response <- userService.instance ! cmd
      result =
        response.fold(
          errs => UnprocessableEntity(toJson(Outcome.fromErrors(errs.toList))),
          {
            case UserEvent.Created(user,_) => Created(toJson(user.withHypermedia))

            case UserEvent.Updated(user,_) => Ok(toJson(user.withHypermedia))

            case UserEvent.Deleted(userId,_) => Ok

          }
        )
    } yield result



}
