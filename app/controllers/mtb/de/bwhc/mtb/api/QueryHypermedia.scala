package de.bwhc.mtb.api


import scala.concurrent.{ExecutionContext,Future}

import play.api.libs.json.JsValue

import de.bwhc.util.json.schema._
import de.bwhc.rest.util.sapphyre._

import de.bwhc.auth.api.UserWithRoles

import de.bwhc.util.syntax.piping._

import de.bwhc.mtb.query.api.{
  PatientView,
  Query,
  QueryOps
}
import de.bwhc.mtb.data.entry.dtos.{
  MTBFile,
  Patient
}
import de.bwhc.mtb.data.entry.views.MTBFileView




trait QueryHypermedia
{

  import syntax._
  import Method._
  import Relations._
  import QuerySchemas._


  private val BASE_URI = "/bwhc/mtb/api/query"

  
  private val SUBMIT_LOCAL_QUERY = "submit-local-query"
  private val SUBMIT_FEDERATED_QUERY = "submit-federated-query"
  private val APPLY_FILTER    = "apply-filter"

  private val QUERY           = "query"
  private val PATIENTS        = "patients"
  private val NGS_SUMMARIES   = "ngs-summaries"
  private val RECOMMENDATIONS = "therapy-recommendations"
  private val THERAPIES       = "molecular-therapies"
  private val MTBFILE         = "mtbfile"
  private val MTBFILEVIEW     = "mtbfileView"



  val ApiBaseLink =
    Link(s"$BASE_URI/")


  private val LocalQueryAction =
    SUBMIT_LOCAL_QUERY -> Action(POST -> BASE_URI)
                            .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$QUERY"))

  private val FederatedQueryAction =
    SUBMIT_FEDERATED_QUERY -> Action(POST -> BASE_URI)
                                .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$QUERY"))

  private def UpdateAction(queryId: Query.Id) =
    UPDATE -> Action(POST -> s"$BASE_URI/${queryId.value}")
                .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$UPDATE"))

  private def ApplyFilterAction(queryId: Query.Id) =
    APPLY_FILTER -> Action(POST -> s"$BASE_URI/${queryId.value}/filter")
                      .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$APPLY_FILTER"))


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

  private def MTBFileViewLink(queryId: Query.Id, patId: Patient.Id) = 
    Link(s"$BASE_URI/${queryId.value}/mtbfileViews/${patId.value}")



  private val schemas =
    Map(
      QUERY        -> JsValueSchema[QueryForm],
      UPDATE       -> JsValueSchema[QueryOps.Command.Update],
      APPLY_FILTER -> JsValueSchema[QueryOps.Command.ApplyFilter],
      PATIENTS     -> JsValueSchema[PatientView],
    )


  def schema(rel: String): Option[JsValue] =
    schemas.get(rel) 



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
      MTBFILE    -> MTBFileLink(queryId,patient.id),
      MTBFILEVIEW -> MTBFileViewLink(queryId,patient.id)
    )

  }


  def HyperMTBFile(
    mtbfile: MTBFile
  )(
    queryId: Query.Id
  ) = {
    mtbfile.withLinks(
      BASE  -> ApiBaseLink,
      SELF  -> MTBFileLink(queryId,mtbfile.patient.id),
      QUERY -> QueryLink(queryId)
    )
  }


  def HyperMTBFileView(
    mtbfile: MTBFileView
  )(
    queryId: Query.Id
  ) = {
    mtbfile.withLinks(
      BASE  -> ApiBaseLink,
      SELF  -> MTBFileLink(queryId,mtbfile.patient.id),
      QUERY -> QueryLink(queryId)
    )
  }



}
object QueryHypermedia extends QueryHypermedia


