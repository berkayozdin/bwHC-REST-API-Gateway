package de.bwhc.mtb.api


import de.bwhc.auth.core._
import de.bwhc.auth.api.UserWithRoles


trait DataManagementPermissions
{

  import de.bwhc.user.api.Role._


  private val AdminRights =
    Authorization[UserWithRoles](_ hasRole Admin)


  private val DocumentaristRights =
    Authorization[UserWithRoles](_ hasRole Documentarist)


  val PatientStatusAccessRights = AdminRights


  val DataQualityAccessRights = DocumentaristRights

}
object DataManagementPermissions extends DataManagementPermissions

