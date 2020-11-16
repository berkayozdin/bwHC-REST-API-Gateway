package de.bwhc.rest.auth


import de.bwhc.user.api.User


trait UserResources
{

  import de.bwhc.rest.util.scapphyre
  import de.bwhc.rest.util.scapphyre._

  import scapphyre.Method._
  import scapphyre.Relations._

  
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


  def HyperUsers[C[X] <: Iterable[X]](users: C[User]) = {
  
    Collection(
      "users",
      users.map(HyperUser)
    )
    .withLinks(
      Self -> Link(baseUrl)
    )
    .withActions(
      Create  -> Action(POST, baseUrl)
    )
     

  }



}
object UserResources extends UserResources
