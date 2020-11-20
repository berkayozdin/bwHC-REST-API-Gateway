package de.bwhc.mtb.api


import scala.concurrent.ExecutionContext

import de.bwhc.mtb.query.api.{Query,QueryService}

import de.bwhc.auth.api._
import de.bwhc.auth.core._
import de.bwhc.auth.core.Authorization._



trait QueryModePermissions
{

  import de.bwhc.user.api.Role._


  val LocalQCAccessRight =
    Authorization[UserWithRoles](
      _ hasAnyOf Set(GlobalZPMCoordinator, LocalZPMCoordinator, MTBCoordinator)
    )
/*
    Authorization[UserWithRoles](user =>
      (user hasRole LocalZPMCoordinator) ||
      (user hasRole GlobalZPMCoordinator) ||
      (user hasRole MTBCoordinator)
    )
*/

  val GlobalQCAccessRight =
    Authorization[UserWithRoles](_ hasRole GlobalZPMCoordinator)


  val FederatedEvidenceQueryRight =
    Authorization[UserWithRoles](
      _ hasAnyOf Set(GlobalZPMCoordinator, Researcher)
    )


  val LocalEvidenceQueryRight =
    Authorization[UserWithRoles](
      _ hasAnyOf Set(GlobalZPMCoordinator, Researcher, LocalZPMCoordinator, MTBCoordinator)
    )

  val EvidenceQueryRight = LocalEvidenceQueryRight


  def QueryRightFor(
    mode: Query.Mode.Value
  ): Authorization[UserWithRoles] =
    if (mode == Query.Mode.Federated) FederatedEvidenceQueryRight
    else LocalEvidenceQueryRight
 

  protected val service: QueryService


  def AccessRightFor(
    queryId: Query.Id
  )(
    implicit ec: ExecutionContext
  ): Authorization[UserWithRoles] = Authorization.async{

    case UserWithRoles(userId,_) =>

      for {
        query <- service get queryId
        ok    =  query.exists(_.querier.value == userId.value)
      } yield ok
  }


}

