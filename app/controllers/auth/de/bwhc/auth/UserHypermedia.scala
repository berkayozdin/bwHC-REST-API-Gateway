package de.bwhc.rest.auth



import scala.concurrent.{ExecutionContext,Future}

import de.bwhc.user.api.User

import de.bwhc.auth.api.UserWithRoles


trait UserHypermedia
{

  import de.bwhc.rest.util.Table
  import de.bwhc.rest.util.sapphyre._
  import de.bwhc.rest.util.sapphyre.syntax._
  import Method._
  import Relations._

  import UserManagementPermissions._


  private val BASE_URI = "/bwhc/user/api"


  val LOGIN        = "login"
  val LOGOUT       = "logout"
  val WHOAMI       = "whoami"
  val USER         = "user"
  val USERS        = "users"
  val UPDATE_ROLES = "update-roles"
  val CHANGE_PASSWORD = "change-password"


  val ApiBaseLink =
   Link(s"$BASE_URI/")

  val UsersLink =
    Link(s"$BASE_URI/$USERS")

  def UserLink(id: User.Id) =
    Link(s"$BASE_URI/$USERS/${id.value}")

  val LoginAction =
    LOGIN -> Action(POST -> s"$BASE_URI/$LOGIN")
               .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$LOGIN"))

  val LogoutAction =
    LOGOUT -> Action(POST -> s"$BASE_URI/$LOGOUT")

  val WhoAmIAction =
    WHOAMI -> Action(GET  -> s"$BASE_URI/$WHOAMI")


  val CreateUserAction =
    CREATE -> Action(POST -> UsersLink.href)
                .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$CREATE"))
  
  def UpdateUserAction(id: User.Id) = 
    UPDATE -> Action(PUT, UserLink(id))
        
  def UpdateRolesAction(id: User.Id) =
    UPDATE_ROLES -> Action(PUT -> s"${UserLink(id).href}/roles")
        
  def ChangePasswordAction(id: User.Id) =
    CHANGE_PASSWORD -> Action(PUT -> s"${UserLink(id).href}/change-password")
        
  def DeleteUserAction(id: User.Id) =
    Relations.DELETE -> Action(Method.DELETE, UserLink(id))



  def ApiResource(
    implicit
    agent: UserWithRoles,
    ec: ExecutionContext
  ) = 
    for {
      api <-
        Future.successful(
          Resource.empty
            .withLinks(
              SELF -> ApiBaseLink,
              USER -> UserLink(agent.userId)
            )
            .withActions(
              LoginAction,
              LogoutAction,
              WhoAmIAction
            )
        )

      canGetUsers <- agent has GetAllUserRights
      
      result =
        if (canGetUsers) api.withLinks(USERS -> UsersLink)
        else api

    } yield result



  import de.bwhc.util.syntax.piping._  

  implicit def userWithHypermedia(
    implicit
    agent: UserWithRoles,
    ec: ExecutionContext
  ) = {

    (user: User) =>

    val hyperUser =
      user.withLinks(
        BASE -> ApiBaseLink,
        SELF -> UserLink(user.id)
      )

    for {
      canUpdate      <- agent has UpdateUserRights(user.id)
      canUpdateRoles <- agent has UpdateUserRolesRights
      canDelete      <- agent has DeleteUserRights
      canGetUsers    <- agent has GetAllUserRights

      actions =
        Seq.empty[(String,Action)] |
        (as => if (canUpdate)      as :+ UpdateUserAction(user.id) :+ ChangePasswordAction(user.id)
               else as) |
        (as => if (canUpdateRoles) as :+ UpdateRolesAction(user.id) 
               else as) |
        (as => if (canDelete)      as :+ DeleteUserAction(user.id)
               else as) 

      result =
        hyperUser.withActions(actions: _*)
     
    } yield if (canGetUsers) result.withLinks(COLLECTION -> UsersLink) else result

  }


  implicit val userHeader: Table.Header[User] =
    Table.Header[User](
      "id"           -> "ID",
      "username"     -> "Username",
      "givenName"    -> "Surname",
      "familyName"   -> "Name",
      "status"       -> "Status",
      "roles"        -> "Roles",
      "registeredOn" -> "Registration Date"
    )


  implicit def usersWithHypermedia[C[X] <: Iterable[X]](
    implicit
    agent: UserWithRoles,
    ec: ExecutionContext
  ) = {

    (users: C[User]) =>

    for {
      items <-
        Future.sequence(users.map(Hyper(_)))

      hyperTable = 
        Table(items)
          .withLinks(
            BASE -> ApiBaseLink,
            SELF -> UsersLink
          )
          .withActions(
            CreateUserAction
          )
     } yield hyperTable

  }

}
object UserHypermedia extends UserHypermedia
