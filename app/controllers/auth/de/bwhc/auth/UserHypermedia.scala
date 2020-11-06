package de.bwhc.rest.auth


import de.bwhc.user.api.User

import de.bwhc.rest.util.cphl._
import de.bwhc.rest.util.cphl.Relations._
import de.bwhc.rest.util.cphl.Method._
import de.bwhc.rest.util.cphl.Action.Format
import de.bwhc.rest.util.cphl.Action.Format._
import de.bwhc.rest.util.cphl.syntax._

import play.api.libs.json.JsValue

import json._
import de.bwhc.util.json.schema._

import de.bwhc.user.api.{
  User,
  Role,
  UserCommand
}

trait Schemas
{

  import de.bwhc.util.json.schema.workarounds._

  implicit val userSchema        = Json.schema[User]  
  implicit val createUserSchema  = Json.schema[UserCommand.Create]  
  implicit val updateUserSchema  = Json.schema[UserCommand.Update]  
  implicit val updateRolesSchema = Json.schema[UserCommand.UpdateRoles]  
  implicit val loginSchema       = Json.schema[Credentials]  

}
object Schemas extends Schemas


trait UserHypermedia
{

  import Schemas._

  val baseUrl = "/bwhc/user/api"

  private val Login       = Relation("login")
  private val Logout      = Relation("logout")
  private val UpdateRoles = Relation("update-roles")

  private val schemaMap =
    Map(
      Login       -> JsValueSchema[Credentials],
      Create      -> JsValueSchema[UserCommand.Create],
      Update      -> JsValueSchema[UserCommand.Update],
      UpdateRoles -> JsValueSchema[UserCommand.UpdateRoles]
    )

  def schemaFor(rel: String): Option[JsValue] =
    schemaMap.get(Relation(rel))


  val userApiActions =
    CPHL.empty[User](
      Base        -> Action(s"$baseUrl",        GET),

      Create      -> Action(s"$baseUrl/user",   POST)
                       .withFormats(
                         JSON -> Format("application/json",s"$baseUrl/schema/${Create.name}")
                       ),

      Update      -> Action(s"$baseUrl/user/ID", PUT)
                       .withFormats(
                         JSON -> Format("application/json",s"$baseUrl/schema/${Update.name}")
                       ),

      UpdateRoles -> Action(s"$baseUrl/user/ID/roles", PUT)
                       .withFormats(
                         JSON -> Format("application/json",s"$baseUrl/schema/${UpdateRoles.name}")
                       ),

      Delete      -> Action(s"$baseUrl/user/ID", DELETE),

      Search      -> Action(s"$baseUrl/user",   GET),

      Login       -> Action(s"$baseUrl/login",  POST)
                       .withFormats(
                         JSON -> Format("application/json",s"$baseUrl/schema/${Login.name}")
                       ),

      Logout      -> Action(s"$baseUrl/logout", POST)
    )


  implicit val userAsCPHL: User => CPHL[User] = {

    user =>

      val id = user.id.value

      user.withActions(
        Self        -> Action(s"$baseUrl/user/$id",       GET),
        Update      -> Action(s"$baseUrl/user/$id",       PUT)
                         .withFormats(
                           JSON -> Format("application/json",s"$baseUrl/schema/${Update.name}")
                         ),
        UpdateRoles -> Action(s"$baseUrl/user/$id/roles", PUT)
                         .withFormats(
                           JSON -> Format("application/json",s"$baseUrl/schema/${UpdateRoles.name}")
                         ),
        Delete      -> Action(s"$baseUrl/user/$id",       DELETE),
        Login       -> Action(s"$baseUrl/login",          POST),
        Logout      -> Action(s"$baseUrl/logout",         POST)
      )

  }

}
object UserHypermedia extends UserHypermedia


