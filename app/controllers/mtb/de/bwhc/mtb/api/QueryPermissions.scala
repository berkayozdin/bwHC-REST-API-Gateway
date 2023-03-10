package de.bwhc.mtb.api


import scala.concurrent.ExecutionContext

import de.bwhc.mtb.query.api.{Query,QueryService}

import de.bwhc.auth.api._
import de.bwhc.auth.core._
import de.bwhc.auth.core.Authorization._



trait QueryPermissions
{

  import de.bwhc.user.api.Role._

  //---------------------------------------------------------------------------
  // ZPM QC Reporting
  //---------------------------------------------------------------------------

  val GlobalQCAccessRight =
    Authorization[UserWithRoles](_ hasRole GlobalZPMCoordinator)

//  val LocalQCAccessRight =
//    GlobalQCAccessRight OR Authorization[UserWithRoles](_ hasAnyOf (LocalZPMCoordinator, MTBCoordinator))

  val LocalQCAccessRight =
    Authorization[UserWithRoles](
      _ hasAnyOf (GlobalZPMCoordinator, LocalZPMCoordinator, MTBCoordinator)
    )

  val QCAccessRight = LocalQCAccessRight


  //---------------------------------------------------------------------------
  // Evidence Queries
  //---------------------------------------------------------------------------

  val FederatedEvidenceQueryRight =
    Authorization[UserWithRoles](
      _ hasAnyOf (GlobalZPMCoordinator, Researcher, ApprovedResearcher)
    )


  val LocalEvidenceQueryRight =
    Authorization[UserWithRoles](
      _ hasAnyOf (GlobalZPMCoordinator, LocalZPMCoordinator, MTBCoordinator, Researcher, ApprovedResearcher)
    )

  val EvidenceQueryRight = LocalEvidenceQueryRight


  def QueryRightFor(
    mode: Query.Mode.Value
  ): Authorization[UserWithRoles] =
    if (mode == Query.Mode.Federated) FederatedEvidenceQueryRight
    else LocalEvidenceQueryRight
 

  def AccessRightFor(
    queryId: Query.Id
  )(
    implicit
    service: QueryService,
    ec: ExecutionContext
  ): Authorization[UserWithRoles] =
    EvidenceQueryRight AND Authorization.async {
      case UserWithRoles(userId,_) =>
        for {
          query <- service get queryId
          ok    =  query.exists(_.querier.value == userId.value)
        } yield ok
    }


  val MTBFileAccessRight =  
    Authorization[UserWithRoles](_ hasAnyOf (MTBCoordinator, ApprovedResearcher))

}
object QueryPermissions extends QueryPermissions
