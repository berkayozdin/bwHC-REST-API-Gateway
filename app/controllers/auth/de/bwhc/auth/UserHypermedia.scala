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


  val baseUrl = "/bwhc/user/api"


  val LOGIN        = "login"
  val LOGOUT       = "logout"
  val WHOAMI       = "whoami"
  val USER         = "user"
  val USERS        = "users"
  val UPDATE_ROLES = "update-roles"


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
              SELF -> Link(s"$baseUrl/"),
              USER -> Link(s"$baseUrl/$USER/${agent.userId.value}")
            )
            .withActions(
              LOGIN  -> Action(POST -> s"$baseUrl/$LOGIN")
                          .withFormats(MediaType.APPLICATION_JSON -> Link(s"$baseUrl/schema/$LOGIN")),
              LOGOUT -> Action(POST -> s"$baseUrl/$LOGOUT"),
              WHOAMI -> Action(GET  -> s"$baseUrl/$WHOAMI")
            )
        )

      canGetUsers <- agent has GetAllUserRights
      
      result =
        if (canGetUsers) api.withLinks(USERS -> Link(s"$baseUrl/$USERS"))
        else api

    } yield result



  import de.bwhc.util.syntax.piping._  

  def UserResource(
    user: User
  )(
    implicit
    agent: UserWithRoles,
    ec: ExecutionContext
  ) = {

    val id = user.id.value

    val hyperUser =
      user.withLinks(
        BASE -> Link(s"$baseUrl/"),
        SELF -> Link(s"$baseUrl/$USERS/$id")
      )

    for {
      canUpdate      <- agent has UpdateUserRights(user.id)
      canUpdateRoles <- agent has UpdateUserRolesRights
      canDelete      <- agent has DeleteUserRights
      canGetUsers    <- agent has GetAllUserRights

      actions =
        Seq.empty[(String,Action)] |
        (as => if (canUpdate)      as :+ (UPDATE           -> Action(PUT           -> s"$baseUrl/$USERS/$id"))
               else as) |
        (as => if (canUpdateRoles) as :+ (UPDATE_ROLES     -> Action(PUT           -> s"$baseUrl/$USERS/$id/roles"))
               else as) |
        (as => if (canDelete)      as :+ (Relations.DELETE -> Action(Method.DELETE -> s"$baseUrl/$USERS/$id"))
               else as) 

      result =
        hyperUser.withActions(actions: _*)
     
    } yield if (canGetUsers) result.withLinks(COLLECTION -> Link(s"$baseUrl/$USERS")) else result

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


  def UsersResource[C[X] <: Iterable[X]](
    users: C[User]
  )(
    implicit
    agent: UserWithRoles,
    ec: ExecutionContext
  ) = {

    for {
      items <-
        Future.sequence(users.map(UserResource(_)))

      hyperTable = 
        Table(items)
          .withLinks(
            BASE -> Link(s"$baseUrl/"),
            SELF -> Link(s"$baseUrl/$USERS")
          )
          .withActions(
            CREATE -> Action(POST -> s"$baseUrl/$USERS")
                        .withFormats(MediaType.APPLICATION_JSON -> Link(s"$baseUrl/schema/$CREATE"))
          )
     } yield hyperTable

  }

}
object UserHypermedia extends UserHypermedia
