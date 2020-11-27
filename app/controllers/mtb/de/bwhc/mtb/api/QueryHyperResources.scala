package de.bwhc.mtb.api


import scala.concurrent.{ExecutionContext,Future}

import de.bwhc.rest.util.sapphyre._

import de.bwhc.auth.api.UserWithRoles

import de.bwhc.util.syntax.piping._

import de.bwhc.mtb.query.api.{Query,PatientView}
import de.bwhc.mtb.data.entry.dtos.Patient


trait QueryHyperResources
{

  import syntax._
  import Method._
  import Relations._


  private val BASE_URI = "/bwhc/mtb/api/query"

  
  private val LOCAL_QUERY     = "submit-local-query"
  private val FEDERATED_QUERY = "submit-federated-query"
  private val APPLY_FILTER    = "apply-filter"

  private val QUERY           = "query"
  private val PATIENTS        = "patients"
  private val NGS_SUMMARIES   = "ngs-summaries"
  private val RECOMMENDATIONS = "therapy-recommendations"
  private val THERAPIES       = "molecular-therapies"
  private val MTBFILE         = "mtbfile"


  private val ApiBaseLink =
    Link(s"$BASE_URI")



  private val LocalQueryAction =
    LOCAL_QUERY -> Action(POST -> BASE_URI)

  private val FederatedQueryAction =
    FEDERATED_QUERY -> Action(POST -> BASE_URI)

  private def UpdateAction(queryId: Query.Id) =
    UPDATE -> Action(POST -> s"$BASE_URI/${queryId.value}")

  private def ApplyFilterAction(queryId: Query.Id) =
    APPLY_FILTER -> Action(POST -> s"$BASE_URI/${queryId.value}/filter")


  private def QueryLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}")

  private def PatientsLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/patients")

  private def NGSSummariesLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/ngs-summaries")

  private def RecommendationsLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/therapy-recommendations")

  private def TherapiesLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/molecular-therapies")

  private def MTBFileLink(queryId: Query.Id, patId: Patient.Id) = 
    Link(s"$BASE_URI/${queryId.value}/mtbfiles/${patId.value}")
//    Link(s"${QueryLink(queryId).href}/patients/${patId.value}/mtbfile")


  def Api(
    implicit
    user: UserWithRoles,
    ec: ExecutionContext
  ) = {
    for {

      localQueryRights     <- user has QueryPermissions.LocalEvidenceQueryRight

      federatedQueryRights <- user has QueryPermissions.FederatedEvidenceQueryRight

      api = Resource.empty.withLinks(SELF -> ApiBaseLink)

      result =
        api |
        (r => if (localQueryRights)     r.withActions(LocalQueryAction)     else r) |
        (r => if (federatedQueryRights) r.withActions(FederatedQueryAction) else r)

    } yield result
  }


  def HyperQuery(
    query: Query
  ) = {
    query.withLinks(
      BASE            -> ApiBaseLink,
      SELF            -> QueryLink(query.id),
      PATIENTS        -> PatientsLink(query.id),
      NGS_SUMMARIES   -> NGSSummariesLink(query.id),
      RECOMMENDATIONS -> RecommendationsLink(query.id),
      THERAPIES       -> TherapiesLink(query.id)
    )
    .withActions(
      UpdateAction(query.id),
      ApplyFilterAction(query.id)
    )

  }


  def HyperPatient(
    patient: PatientView
  )(
    queryId: Query.Id
  ) = {
    patient.withLinks(
      BASE       -> ApiBaseLink,
      QUERY      -> QueryLink(queryId),
      COLLECTION -> PatientsLink(queryId),
      MTBFILE    -> Link(s"${QueryLink(queryId).href}/mtbfile/${patient.id.value}")
    )

  }


}
object QueryHyperResources extends QueryHyperResources


