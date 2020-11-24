package de.bwhc.rest.auth



import scala.concurrent.{ExecutionContext,Future}

import de.bwhc.user.api.User

import de.bwhc.auth.api.UserWithRoles


trait UserHypermedia
{

  import de.bwhc.rest.util.sapphyre._
  import Method._
  import Relations._

  import UserManagementPermissions._


  val baseUrl = "/bwhc/user/api"


  def ApiResource(
    agent: UserWithRoles
  )(
    implicit
    ec: ExecutionContext
  ) = {

    for {
      api <-
        Future.successful(
          Resource.empty
            .withLinks(
              Self       -> Link(s"$baseUrl/"),
              "user" -> Link(s"$baseUrl/users/${agent.userId.value}")
            )
            .withActions(
              "login"  -> Action(POST, s"$baseUrl/login"),
              "logout" -> Action(POST, s"$baseUrl/logout")
            )
        )
/*      
      canGetAllUsers <- agent has GetAllUserRights
      
      result =
        if (canGetAllUsers) api.withLinks("users" -> Link(s"$baseUrl/users")) else api
*/      
      result <- (agent has GetAllUserRights)
                  .map(if (_) api.withLinks("users" -> Link(s"$baseUrl/users")) else api)
    } yield result

  }


  import de.bwhc.util.syntax.piping._  

  def UserResource(
    user: User
  )(
    agent: UserWithRoles
  )(
    implicit
    ec: ExecutionContext
  ) = {

    val id = user.id.value

    val resource =
      Resource(user)
        .withLinks(Self -> Link(s"$baseUrl/users/$id"))

    for {
      canUpdate      <- agent has UpdateUserRights(user.id)
      canUpdateRoles <- agent has UpdateUserRolesRights
      canDelete      <- agent has DeleteUserRights

      actions =
        Seq.empty[(String,Action)] |
        (as => if (canUpdate)      as :+ (Update         -> Action(PUT,    s"$baseUrl/users/$id"))       else as) |
        (as => if (canUpdateRoles) as :+ ("update-roles" -> Action(PUT,    s"$baseUrl/users/$id/roles")) else as) |
        (as => if (canDelete)      as :+ (Delete         -> Action(DELETE, s"$baseUrl/users/$id"))       else as) 

      result =
        resource.withActions(actions: _*)

    } yield result

  }


  implicit val userHeader: Table.Header[User] =
    Table.Header[User](
      "id"           -> "ID",
      "username"     -> "Username",
      "humanName"    -> "Name",
      "status"       -> "Status",
      "roles"        -> "Roles",
      "registeredOn" -> "Registration Date"
    )

/*
  implicit val userHeader: Table.Header[User] =
    Table.Header[User](
      Symbol("id")           -> "ID",
      Symbol("username")     -> "Username",
      Symbol("humanName")    -> "Name",
      Symbol("status")       -> "Status",
      Symbol("roles")        -> "Roles",
      Symbol("registeredOn") -> "Registration Date"
    )
*/


  def UsersResource[C[X] <: Iterable[X]](
    users: C[User]
  )(
    agent: UserWithRoles
  )(
    implicit ec: ExecutionContext
  ) = {

    for {
      items <-
        Future.sequence(users.map(UserResource(_)(agent)))

      table = 
        Table(items)
          .withLinks(
            Self -> Link(s"$baseUrl/users")
          )
          .withActions(
            Create -> Action(POST,s"$baseUrl/users")
          )
     } yield table

  }

}
object UserHypermedia extends UserHypermedia
