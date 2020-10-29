package de.bwhc.rest.auth


import de.bwhc.user.api.User

import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.Relations._
import de.bwhc.rest.util.cphl.Method._
import de.bwhc.rest.util.cphl.syntax._


trait UserHypermedia
{

  val baseUrl = "/bwhc/user/api"


  private val Login  = Relation("login")
  private val Logout = Relation("logout")


  val userApiActions =
    CPHL.empty[User](
      Self   -> Action(s"$baseUrl",        GET),
      Create -> Action(s"$baseUrl/user",   POST),
      Search -> Action(s"$baseUrl/user",   GET),
      Login  -> Action(s"$baseUrl/login",  POST),
      Logout -> Action(s"$baseUrl/logout", POST)
    )


  implicit val userAsCPHL: User => CPHL[User] = {

    user =>

      val User.Id(id) = user.id

      user.withActions(
        Self                    -> Action(s"$baseUrl/user/${id}",       GET),
        Update                  -> Action(s"$baseUrl/user/${id}",       PUT),
        Relation("updateRoles") -> Action(s"$baseUrl/user/${id}/roles", PUT),
        Delete                  -> Action(s"$baseUrl/user/${id}",       DELETE),
        Login                   -> Action(s"$baseUrl/login",            POST),
        Logout                  -> Action(s"$baseUrl/logout",           POST)
      )

  }

}
