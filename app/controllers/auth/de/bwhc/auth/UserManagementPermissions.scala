package de.bwhc.rest.auth


import de.bwhc.user.api.{User,Role}

import de.bwhc.auth.core.Authorization
import de.bwhc.auth.api.UserWithRoles


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
object UserManagementPermissions extends UserManagementPermissions


