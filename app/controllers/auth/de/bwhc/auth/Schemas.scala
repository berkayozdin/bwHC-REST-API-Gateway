package de.bwhc.rest.auth



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

  import de.bwhc.rest.util.sapphyre.Relations._
  import UserHypermedia._


  implicit val userSchema        = Json.schema[User]  
  implicit val createUserSchema  = Json.schema[UserCommand.Create]  
  implicit val updateUserSchema  = Json.schema[UserCommand.Update]  
  implicit val updateRolesSchema = Json.schema[UserCommand.UpdateRoles]  
  implicit val loginSchema       = Json.schema[Credentials]  



  private val schemaMap =
    Map(
      LOGIN        -> JsValueSchema[Credentials],
      CREATE       -> JsValueSchema[UserCommand.Create],
      UPDATE       -> JsValueSchema[UserCommand.Update],
      UPDATE_ROLES -> JsValueSchema[UserCommand.UpdateRoles]
    )

  def forRelation(rel: String): Option[JsValue] =
    schemaMap.get(rel)

}
object Schemas extends Schemas

