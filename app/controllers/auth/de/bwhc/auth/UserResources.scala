package de.bwhc.rest.auth


import de.bwhc.user.api.User


trait UserResources
{

  import de.bwhc.rest.util.sapphyre
  import de.bwhc.rest.util.sapphyre._

  import sapphyre.Method._
  import sapphyre.Relations._

  
  val baseUrl = "/bwhc/user/api/users"


  def HyperUser(user: User) = {

    val id = user.id.value

    Resource(user)
      .withLinks(
        Self -> Link(s"$baseUrl/$id")
      )
      .withActions(
        Update         -> Action(PUT,    s"$baseUrl/$id"),
        "update-roles" -> Action(PUT,    s"$baseUrl/$id/roles"),
        Delete         -> Action(DELETE, s"$baseUrl/$id")
      )

  }


/*
final case class User
(
  id: User.Id,
  username: User.Name,
  humanName: HumanName,
  status: User.Status.Value,
  roles: Set[Role.Value],
  registeredOn: LocalDate,
  lastUpdate: Instant
)
*/

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


  def HyperUsers[C[X] <: Iterable[X]](users: C[User]) = {
    Table(
      users.map(HyperUser)
    )
    .withLinks(
      Self -> Link(baseUrl)
    )
    .withActions(
      Create -> Action(POST, baseUrl)
    )
     

  }



}
object UserResources extends UserResources
